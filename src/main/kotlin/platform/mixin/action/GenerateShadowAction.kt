/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.findFields
import com.demonwav.mcdev.platform.mixin.util.findMethods
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findFirstMember
import com.demonwav.mcdev.util.findLastChild
import com.demonwav.mcdev.util.findNextMember
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInsight.generation.GenerationInfo
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.codeInsight.generation.OverrideImplementsAnnotationsHandler
import com.intellij.codeInsight.generation.PsiElementClassMember
import com.intellij.codeInsight.generation.PsiFieldMember
import com.intellij.codeInsight.generation.PsiGenerationInfo
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import java.util.stream.Stream
import kotlin.streams.toList

class GenerateShadowAction : MixinCodeInsightAction() {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val offset = editor.caretModel.offset
        val psiClass = file.findElementAt(offset)?.findContainingClass() ?: return

        val fields = findFields(psiClass)?.map(::PsiFieldMember) ?: emptySequence()
        val methods = findMethods(psiClass)?.map(::PsiMethodMember) ?: emptySequence()

        val members = (fields + methods).toTypedArray()
        if (members.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor, "No members to shadow have been found")
            return
        }

        val chooser = MemberChooser<PsiElementClassMember<*>>(members, false, true, project)
        chooser.title = "Select members to Shadow"
        chooser.setCopyJavadocVisible(false)
        chooser.show()

        val elements = (chooser.selectedElements ?: return).ifEmpty { return }
        disableAnnotationWrapping(project) {
            runWriteAction {
                GenerateMembersUtil.insertMembersAtOffset(
                    file,
                    offset,
                    createShadowMembers(
                        project,
                        psiClass,
                        elements.stream().map(PsiElementClassMember<*>::getElement)
                    )
                    // Select first element in editor
                ).firstOrNull()?.positionCaret(editor, false)
            }
        }
    }
}

fun insertShadows(project: Project, psiClass: PsiClass, members: Stream<PsiMember>) {
    insertShadows(psiClass, createShadowMembers(project, psiClass, members))
}

fun insertShadows(psiClass: PsiClass, shadows: List<GenerationInfo>) {
    // Find first element after shadow
    val lastShadow = psiClass.findLastChild {
        (it as? PsiModifierListOwner)?.modifierList?.findAnnotation(MixinConstants.Annotations.SHADOW) != null
    }

    val anchor = lastShadow?.findNextMember() ?: psiClass.findFirstMember()

    // Insert new shadows after last shadow (or at the top of the class)
    GenerateMembersUtil.insertMembersBeforeAnchor(psiClass, anchor, shadows)
}

fun createShadowMembers(
    project: Project,
    psiClass: PsiClass,
    members: Stream<PsiMember>
): List<PsiGenerationInfo<PsiMember>> {
    var methodAdded = false

    val result = members.map { m ->
        val shadowMember: PsiMember = when (m) {
            is PsiMethod -> {
                methodAdded = true
                shadowMethod(psiClass, m)
            }
            is PsiField -> shadowField(project, m)
            else -> throw UnsupportedOperationException("Unsupported member type: ${m::class.java.name}")
        }

        // Add @Shadow annotation
        shadowMember.modifierList!!.addAnnotation(MixinConstants.Annotations.SHADOW)

        PsiGenerationInfo(shadowMember)
    }.toList()

    // Make the class abstract (if not already)
    if (methodAdded && !psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
        val classModifiers = psiClass.modifierList!!
        if (classModifiers.hasModifierProperty(PsiModifier.FINAL)) {
            classModifiers.setModifierProperty(PsiModifier.FINAL, false)
        }
        classModifiers.setModifierProperty(PsiModifier.ABSTRACT, true)
    }

    return result
}

private fun shadowMethod(psiClass: PsiClass, method: PsiMethod): PsiMethod {
    val newMethod = GenerateMembersUtil.substituteGenericMethod(method, PsiSubstitutor.EMPTY, psiClass)

    // Remove Javadocs
    OverrideImplementUtil.deleteDocComment(newMethod)

    val newModifiers = newMethod.modifierList

    // Relevant modifiers are copied by GenerateMembersUtil.substituteGenericMethod

    // Copy annotations
    copyAnnotations(psiClass.containingFile, method.modifierList, newModifiers)

    // If the method was original private, make it protected now
    if (newModifiers.hasModifierProperty(PsiModifier.PRIVATE)) {
        newModifiers.setModifierProperty(PsiModifier.PROTECTED, true)
    }

    // Make method abstract
    newModifiers.setModifierProperty(PsiModifier.ABSTRACT, true)

    // Remove code block
    newMethod.body?.delete()

    return newMethod
}

private fun shadowField(project: Project, field: PsiField): PsiField {
    val newField = JavaPsiFacade.getElementFactory(project).createField(field.name, field.type)
    val newModifiers = newField.modifierList!!

    val modifiers = field.modifierList!!

    // Copy modifiers
    copyModifiers(modifiers, newModifiers)

    // Copy annotations
    copyAnnotations(field.containingFile, modifiers, newModifiers)

    if (newModifiers.hasModifierProperty(PsiModifier.FINAL)) {
        // If original field was final, add the @Final annotation instead
        newModifiers.setModifierProperty(PsiModifier.FINAL, false)
        newModifiers.addAnnotation(MixinConstants.Annotations.FINAL)
    }

    return newField
}

private fun copyModifiers(modifiers: PsiModifierList, newModifiers: PsiModifierList) {
    for (modifier in PsiModifier.MODIFIERS) {
        if (modifiers.hasExplicitModifier(modifier)) {
            newModifiers.setModifierProperty(modifier, true)
        }
    }
}

private fun copyAnnotations(file: PsiFile, modifiers: PsiModifierList, newModifiers: PsiModifierList) {
    // Copy annotations registered by extensions (e.g. @Nullable), based on OverrideImplementUtil.annotateOnOverrideImplement
    for (ext in OverrideImplementsAnnotationsHandler.EP_NAME.extensionList) {
        for (annotation in ext.getAnnotations(file)) {
            copyAnnotation(modifiers, newModifiers, annotation)
        }
    }

    // Copy @Deprecated annotation
    copyAnnotation(modifiers, newModifiers, CommonClassNames.JAVA_LANG_DEPRECATED)
}

private fun copyAnnotation(modifiers: PsiModifierList, newModifiers: PsiModifierList, annotation: String) {
    // Check if annotation exists
    val psiAnnotation = modifiers.findAnnotation(annotation) ?: return
    // Have we already added this annotation? If not, copy it
    newModifiers.findAnnotation(annotation) ?: newModifiers.addAfter(psiAnnotation, null)
}

inline fun disableAnnotationWrapping(project: Project, func: () -> Unit) {
    val settings = CodeStyle.getSettings(project).getCommonSettings(JavaLanguage.INSTANCE)
    val methodWrap = settings.METHOD_ANNOTATION_WRAP
    val fieldWrap = settings.FIELD_ANNOTATION_WRAP
    settings.METHOD_ANNOTATION_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP
    settings.FIELD_ANNOTATION_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

    try {
        func()
    } finally {
        settings.METHOD_ANNOTATION_WRAP = methodWrap
        settings.FIELD_ANNOTATION_WRAP = fieldWrap
    }
}
