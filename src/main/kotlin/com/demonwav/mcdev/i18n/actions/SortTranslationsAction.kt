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

import com.demonwav.mcdev.i18n.I18nConstants
import com.demonwav.mcdev.i18n.findDefaultLangFile
import com.demonwav.mcdev.i18n.intentions.SortTranslationsIntention
import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.demonwav.mcdev.util.mcDomain
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys

class SortTranslationsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.PSI_FILE) ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val defaultFileMissing = e.project?.findDefaultLangFile(file.virtualFile.mcDomain ?: return) == null
        val isDefaultFile = file.name == I18nConstants.DEFAULT_LOCALE_FILE
        val (order, comments) = TranslationSortOrderDialog.show(defaultFileMissing || isDefaultFile)
        if (order == null) {
            return
        }
        SortTranslationsIntention(order, comments).invoke(editor.project ?: return, editor, file)
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

