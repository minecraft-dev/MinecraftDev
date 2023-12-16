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

package com.demonwav.mcdev.translations

import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.sorting.Ordering
import com.demonwav.mcdev.translations.sorting.TranslationSorter
import com.demonwav.mcdev.util.applyWriteAction
import com.demonwav.mcdev.util.findMcpModule
import com.demonwav.mcdev.util.mcDomain
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.util.function.Function
import javax.swing.JComponent

class TranslationEditorNotificationProvider : EditorNotificationProvider {
    private var show: Boolean = true

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (!show || !TranslationFiles.isTranslationFile(file) || TranslationFiles.isDefaultLocale(file)) {
            return null
        }

        val missingTranslations = getMissingTranslations(project, file)
        if (missingTranslations.none()) {
            return null
        }

        val hasDefaultTranslations = TranslationIndex.hasDefaultTranslations(project, file.mcDomain)
        val hasMcpModule = file.findPsiFile(project)?.findMcpModule() != null
        return Function {
            createNotificationPanel(missingTranslations, hasDefaultTranslations, hasMcpModule, file, project)
        }
    }

    private fun createNotificationPanel(
        missingTranslations: Sequence<Translation>,
        hasDefaultTranslations: Boolean,
        hasMcpModule: Boolean,
        file: VirtualFile,
        project: Project
    ): InfoPanel {
        val panel = InfoPanel()
        panel.text = "Translation file doesn't match default one (${TranslationConstants.DEFAULT_LOCALE} locale)."
        panel.createActionLabel(
            "Add missing default entries (won't reflect changes in original English localization)",
        ) {
            val psi = PsiManager.getInstance(project).findFile(file) ?: return@createActionLabel
            psi.applyWriteAction {
                val fileEntries = missingTranslations.map {
                    TranslationFiles.FileEntry.Translation(it.key, it.text)
                }
                TranslationFiles.addAll(psi, fileEntries.asIterable())
                EditorNotifications.updateAll()
            }

            if (!hasMcpModule) {
                // TranslationSorter.query requires an MCP module to work
                return@createActionLabel
            }

            val sort = MessageDialogBuilder.yesNo("Sort Translations", "Would you like to sort all translations now?")
                .ask(project)
            if (sort) {
                try {
                    TranslationSorter.query(
                        project,
                        psi,
                        hasDefaultTranslations,
                        Ordering.LIKE_DEFAULT
                    )
                } catch (e: Exception) {
                    Notification(
                        "Translations sorting error",
                        "Error sorting translations",
                        e.message ?: e.stackTraceToString(),
                        NotificationType.WARNING,
                    ).notify(project)
                }
            }
        }
        panel.createActionLabel("Hide notification") {
            panel.isVisible = false
            show = false
        }
        return panel
    }

    private fun getMissingTranslations(project: Project, file: VirtualFile): Sequence<Translation> {
        val domain = file.mcDomain

        val defaultTranslations = TranslationIndex.getProjectDefaultTranslations(project, domain)
        val translations = TranslationIndex.getTranslations(project, file)

        val keys = translations.map { it.key }.toSet()

        return defaultTranslations.filter { it.key !in keys }
    }

    class InfoPanel : EditorNotificationPanel() {
        override fun getBackground(): Color {
            val color = EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.NOTIFICATION_BACKGROUND)
            return color ?: UIUtil.getPanelBackground()
        }
    }
}
