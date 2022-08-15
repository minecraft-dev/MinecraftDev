/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.action

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.PsiUtilBase

class GenerateAccessorAction : BaseGenerateAction(GenerateAccessorHandler()) {
    /**
     * Copied from [com.intellij.codeInsight.actions.CodeInsightAction.actionPerformedImpl]
     * except that it calls the [GenerateAccessorHandler.customInvoke] method instead of the normal one
     */
    override fun actionPerformedImpl(project: Project, editor: Editor?) {
        if (editor == null) {
            return
        }
        val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return
        val handler = this.handler as GenerateAccessorHandler
        val elementToMakeWritable = handler.getElementToMakeWritable(psiFile)
        if (elementToMakeWritable != null) {
            if (!EditorModificationUtil.checkModificationAllowed(editor) ||
                !FileModificationService.getInstance().preparePsiElementsForWrite(elementToMakeWritable)
            ) {
                return
            }
        }

        CommandProcessor.getInstance().executeCommand(
            project,
            {
                val action = Runnable {
                    if (ApplicationManager.getApplication().isHeadlessEnvironment ||
                        editor.contentComponent.isShowing
                    ) {
                        handler.customInvoke(project, editor, psiFile)
                    }
                }
                if (handler.startInWriteAction()) {
                    ApplicationManager.getApplication().runWriteAction(action)
                } else {
                    action.run()
                }
            },
            commandName,
            DocCommandGroupId.noneGroupId(editor.document),
            editor.document
        )
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file !is PsiJavaFile) {
            return false
        }

        val targetClass = getTargetClass(editor, file)
        return targetClass != null && isValidForClass(targetClass)
    }
}
