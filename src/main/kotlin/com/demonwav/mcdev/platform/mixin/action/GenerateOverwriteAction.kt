/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.findMethods
import com.demonwav.mcdev.platform.mixin.util.findSource
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory.Companion.MIXIN_OVERWRITE_FALLBACK
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.codeInsight.generation.PsiGenerationInfo
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.CodeStyleManager

class GenerateOverwriteAction : MixinCodeInsightAction() {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val offset = editor.caretModel.offset
        val psiClass = file.findElementAt(offset)?.findContainingClass() ?: return
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

        val elements = (chooser.selectedElements ?: return).ifEmpty { return }

        val requiredMembers = LinkedHashSet<PsiMember>()

        runWriteAction {
            val newMethods = elements.map {
                val method = it.element.findSource()
                val sourceClass = method.containingClass
                val codeBlock = method.body

                val newMethod: PsiMethod
                if (sourceClass != null && codeBlock != null) {
                    // Source of method is available

                    // Collect all references to potential @Shadow members
                    requiredMembers.addAll(collectRequiredMembers(codeBlock, sourceClass))
                }

                // Create temporary (dummy) method
                var tmpMethod =
                    JavaPsiFacade.getElementFactory(project).createMethod(method.name, method.returnType!!, psiClass)

                // Replace temporary method with a copy of the original method
                tmpMethod = tmpMethod.replace(method) as PsiMethod

                // Remove Javadocs
                OverrideImplementUtil.deleteDocComment(tmpMethod)

                // Reformat the code with the project settings
                newMethod = CodeStyleManager.getInstance(project).reformat(tmpMethod) as PsiMethod

                if (codeBlock == null) {
                    // Generate fallback method body if source is not available
                    OverrideImplementUtil.setupMethodBody(
                        newMethod, method, psiClass,
                        FileTemplateManager.getInstance(project).getCodeTemplate(MIXIN_OVERWRITE_FALLBACK)
                    )
                }

                // TODO: Automatically add Javadoc comment for @Overwrite? - yes please

                // Add @Overwrite annotation
                newMethod.modifierList.addAnnotation(MixinConstants.Annotations.OVERWRITE)
                PsiGenerationInfo(newMethod)
            }

            // Insert new methods
            GenerateMembersUtil.insertMembersAtOffset(file, offset, newMethods)
                // Select first element in editor
                .first().positionCaret(editor, true)
        }

        // Generate needed shadows
        val newShadows = createShadowMembers(project, psiClass, filterNewShadows(requiredMembers, psiClass))

        disableAnnotationWrapping(project) {
            runWriteAction {
                // Insert shadows
                insertShadows(psiClass, newShadows)
            }
        }
    }
}
