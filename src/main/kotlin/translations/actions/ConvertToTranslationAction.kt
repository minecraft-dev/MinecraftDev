/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.actions

import com.demonwav.mcdev.translations.intentions.ConvertToTranslationIntention
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.PsiLiteral

class ConvertToTranslationAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.PSI_FILE) ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val element = file.findElementAt(editor.caretModel.offset) ?: return
        ConvertToTranslationIntention().invoke(editor.project ?: return, editor, element)
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (file == null || editor == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val element = file.findElementAt(editor.caretModel.offset)
        e.presentation.isEnabledAndVisible = (element?.parent as? PsiLiteral)?.value is String
    }
}
