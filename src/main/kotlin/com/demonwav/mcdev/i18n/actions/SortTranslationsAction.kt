/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.actions

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.i18n.intentions.SortTranslationsIntention
import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages

class SortTranslationsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.PSI_FILE) ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val values = arrayOf("Ascending", "Descending", "Like default (en_us.lang)")
        val i = Messages.showChooseDialog("Sort order:", "Select Sort Order", values, "Ascending", null)
        val order = SortTranslationsIntention.Ordering.values()[i]
        SortTranslationsIntention(order).invoke(editor.project ?: return, editor, file)
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (file == null || editor == null) {
            e.presentation.isEnabled = false
            return
        }
        e.presentation.isEnabled = file.fileType === I18nFileType
    }
}

