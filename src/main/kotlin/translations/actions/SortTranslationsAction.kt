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

import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.sorting.TranslationSorter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys

class SortTranslationsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.PSI_FILE) ?: return
        TranslationSorter.query(file.project, file)
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.VIRTUAL_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (file == null || editor == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        e.presentation.isEnabledAndVisible = TranslationFiles.isTranslationFile(file)
    }
}
