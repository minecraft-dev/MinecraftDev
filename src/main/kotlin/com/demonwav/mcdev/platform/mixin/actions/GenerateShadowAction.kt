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
import com.demonwav.mcdev.util.getClassOfElement
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInsight.generation.PsiElementClassMember
import com.intellij.codeInsight.generation.PsiFieldMember
import com.intellij.codeInsight.generation.PsiGenerationInfo
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import java.util.stream.Stream

class GenerateShadowAction : MixinCodeInsightAction() {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val offset = editor.caretModel.offset
        val psiClass = getClassOfElement(file.findElementAt(offset)) ?: return

        val fields = (findFields(psiClass) ?: Stream.empty())
                .map(::PsiFieldMember)

        val methods = (findMethods(psiClass) ?: Stream.empty())
                .map(::PsiMethodMember)

        val members = Stream.concat(fields, methods).toTypedArray()

        if (members.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor, "No members to shadow have been found")
            return
        }

        val chooser = MemberChooser<PsiElementClassMember<*>>(members, false, true, project)
        chooser.title = "Select members to Shadow"
        chooser.setCopyJavadocVisible(false)
        chooser.show()

        val elements = chooser.selectedElements ?: return
        if (elements.isEmpty()) {
            return
        }

        runWriteAction {
            GenerateMembersUtil.insertMembersAtOffset(file, offset, createShadowMembers(project, psiClass,
                    elements.map(PsiElementClassMember<*>::getElement)))

                    // Select first element in editor
                    .first().positionCaret(editor, false)
        }
    }

}

fun createShadowMembers(project: Project, psiClass: PsiClass, members: List<PsiMember>): List<PsiGenerationInfo<PsiMember>> {
    var methodAdded = false

    val result = members.map {
        val shadowMember = copyMember(project, psiClass, it)

        when (shadowMember) {
            is PsiMethod -> {
                methodAdded = true

                // If the method was original private, make it protected now
                if (shadowMember.modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
                    shadowMember.modifierList.setModifierProperty(PsiModifier.PROTECTED, true)
                }

                // Make method abstract
                shadowMember.modifierList.setModifierProperty(PsiModifier.ABSTRACT, true)

                // Remove code block
                if (shadowMember.lastChild is PsiCodeBlock) {
                    shadowMember.lastChild.delete()
                }
            }
            is PsiField -> {
                if (it.modifierList!!.hasModifierProperty(PsiModifier.FINAL)) {
                    // If original field was final, add the @Final annotation
                    shadowMember.modifierList!!.addAnnotation(MixinConstants.Annotations.FINAL)
                }
            }
        }

        shadowMember.modifierList!!.addAnnotation(MixinConstants.Annotations.SHADOW)

        PsiGenerationInfo(shadowMember)
    }

    // Make the class abstract (if not already)
    if (methodAdded && !psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
        psiClass.modifierList?.setModifierProperty(PsiModifier.ABSTRACT, true)
    }

    return result
}
