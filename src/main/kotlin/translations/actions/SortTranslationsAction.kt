/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.translations.actions

import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.sorting.TranslationSorter
import com.demonwav.mcdev.util.mcDomain
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys

class SortTranslationsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.PSI_FILE) ?: return

        try {
            TranslationSorter.query(
                file.project,
                file,
                TranslationIndex.hasDefaultTranslations(file.project, file.virtualFile.mcDomain)
            )
        } catch (e: Exception) {
            Notification(
                "Translations sorting error",
                "Error sorting translations",
                e.message ?: e.stackTraceToString(),
                NotificationType.WARNING,
            ).notify(file.project)
        }
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
