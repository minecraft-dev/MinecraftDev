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
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.codeInsight.generation.PsiGenerationInfo
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.JavaTemplateUtil
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class GenerateOverwriteAction : MixinCodeInsightAction() {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val offset = editor.caretModel.offset
        val psiClass = getClassOfElement(file.findElementAt(offset)) ?: return
        val methods = (findMethods(psiClass) ?: return)
                .map(::PsiMethodMember).toTypedArray()

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
                val overwriteMethod = copyMethod(project, psiClass, it.element)

                // Generate method body
                OverrideImplementUtil.setupMethodBody(overwriteMethod, it.element, psiClass,
                        FileTemplateManager.getInstance(project).getCodeTemplate(JavaTemplateUtil.TEMPLATE_IMPLEMENTED_METHOD_BODY))

                overwriteMethod.modifierList.addAnnotation(MixinConstants.Annotations.OVERWRITE)
                PsiGenerationInfo(overwriteMethod)
            })
                    // Select first element in editor
                    .first().positionCaret(editor, true)
        }
    }

}
