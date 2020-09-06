/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.sideonly

import com.demonwav.mcdev.MinecraftSettings
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ApplyIntentionAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.awt.RelativePoint
import java.awt.event.MouseEvent

class SideOnlyLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!MinecraftSettings.instance.isShowSideOnlyGutterIcons) {
            return null
        }
        if (element !is PsiIdentifier) {
            return null
        }
        val listOwner = element.parent as? PsiModifierListOwner ?: return null
        val implicitHard = SideOnlyUtil.getInferredAnnotationOnly(listOwner, SideHardness.HARD)
        val implicitSoft = SideOnlyUtil.getInferredAnnotationOnly(listOwner, SideHardness.SOFT)
        val implicitAnnotation = implicitHard ?: implicitSoft ?: return null

        var message = "Implicit "
        message += if (implicitHard == null) {
            "soft"
        } else {
            "hard"
        }
        message += "-sided annotation available: " + implicitAnnotation.reason
        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.Gutter.ExtAnnotation,
            { message },
            this::navigate,
            GutterIconRenderer.Alignment.RIGHT
        )
    }

    private fun navigate(event: MouseEvent, element: PsiElement) {
        val listOwner = element.parent
        val containingFile = listOwner.containingFile
        val virtualFile = PsiUtilCore.getVirtualFile(listOwner)

        if (virtualFile != null && containingFile != null) {
            val project = listOwner.project
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            if (editor != null) {
                editor.caretModel.moveToOffset(element.textOffset)
                val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
                if (file != null && virtualFile == file.virtualFile) {
                    val popup = createActionGroupPopup(containingFile, project, editor)
                    popup?.show(RelativePoint(event))
                }
            }
        }
    }

    private fun createActionGroupPopup(file: PsiFile, project: Project, editor: Editor): JBPopup? {
        val intention = MakeInferredMcdevAnnotationExplicit()
        val action = ApplyIntentionAction(intention, intention.text, editor, file)
        val group = DefaultActionGroup(action)
        val context = SimpleDataContext.getProjectContext(null)
        return JBPopupFactory.getInstance()
            .createActionGroupPopup(null, group, context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true)
    }
}
