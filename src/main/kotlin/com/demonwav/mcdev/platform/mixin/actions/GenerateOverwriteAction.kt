/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.actions

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.findMethodsByInternalNameAndDescriptor
import com.demonwav.mcdev.util.getClassOfElement
import com.demonwav.mcdev.util.internalNameAndDescriptor
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.actions.SimpleCodeInsightAction
import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.codeInsight.generation.PsiGenerationInfo
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.JavaTemplateUtil
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.util.containers.isEmpty
import com.intellij.util.containers.stream
import java.util.stream.Collectors
import java.util.stream.Stream

class GenerateOverwriteAction : SimpleCodeInsightAction() {

    override fun startInWriteAction(): Boolean = false

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file.language != JavaLanguage.INSTANCE) {
            return false
        }

        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        return MixinUtils.getContainingMixinClass(element) != null
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val offset = editor.caretModel.offset
        val psiClass = getClassOfElement(file.findElementAt(offset)) ?: return
        val methods = findOverwriteableMethods(psiClass)
                ?.map(::PsiMethodMember)
                ?.toTypedArray() ?: return

        if (methods.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor, "No methods to overwrite have been found")
            return
        }

        val chooser = MemberChooser<PsiMethodMember>(methods, false, true, project)
        chooser.title = "Select Methods to Overwrite"
        chooser.setCopyJavadocVisible(false)
        chooser.show()

        val elements = chooser.selectedElements ?: return
        if (elements.isEmpty()) {
            return
        }

        runWriteAction {
            GenerateMembersUtil.insertMembersAtOffset(file, offset, elements.map {
                val overwriteMethod = GenerateMembersUtil.substituteGenericMethod(it.element, PsiSubstitutor.EMPTY, psiClass)
                overwriteMethod.modifierList.addAnnotation(MixinConstants.Annotations.OVERWRITE)

                // Generate method body
                OverrideImplementUtil.setupMethodBody(overwriteMethod, it.element, psiClass,
                        FileTemplateManager.getInstance(project).getCodeTemplate(JavaTemplateUtil.TEMPLATE_IMPLEMENTED_METHOD_BODY))

                PsiGenerationInfo(overwriteMethod)
            })
                    // Select first element in editor
                    .first().positionCaret(editor, true)
        }
    }

    private fun findOverwriteableMethods(psiClass: PsiClass): Stream<PsiMethod>? {
        val targets = MixinUtils.getAllMixedClasses(psiClass).values
        return when (targets.size) {
            0 -> null
            1 -> targets.single().methods.stream()
                    .filter({!it.isConstructor})
            else -> targets.stream()
                    .flatMap { target -> target.methods.stream() }
                    .filter({!it.isConstructor})
                    .collect(Collectors.groupingBy(PsiMethod::internalNameAndDescriptor))
                    .values.stream()
                    .filter { it.size >= targets.size }
                    .map { it.first() }
        }?.filter {
            // Filter methods which are already in the Mixin class
            psiClass.findMethodsByInternalNameAndDescriptor(it.internalNameAndDescriptor).isEmpty()
        }
    }

}
