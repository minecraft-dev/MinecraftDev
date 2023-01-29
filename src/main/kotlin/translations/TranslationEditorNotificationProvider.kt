/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations

import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.sorting.Ordering
import com.demonwav.mcdev.translations.sorting.TranslationSorter
import com.demonwav.mcdev.util.applyWriteAction
import com.demonwav.mcdev.util.findMcpModule
import com.demonwav.mcdev.util.mcDomain
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
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
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> = Function { createNotificationPanel(file, project) }

    private fun createNotificationPanel(file: VirtualFile, project: Project): InfoPanel? {
        val locale = TranslationFiles.getLocale(file)
        if (!show || !TranslationFiles.isTranslationFile(file) || locale == TranslationConstants.DEFAULT_LOCALE) {
            return null
        }

        val missingTranslations = getMissingTranslations(project, file)
        if (missingTranslations.any()) {
            val panel = InfoPanel()
            panel.setText("Translation file doesn't match default one (${TranslationConstants.DEFAULT_LOCALE} locale).")
            panel.createActionLabel(
                "Add missing default entries (won't reflect changes in original English localization)"
            ) {
                val psi = PsiManager.getInstance(project).findFile(file) ?: return@createActionLabel
                psi.applyWriteAction {
                    val fileEntries = missingTranslations.map {
                        TranslationFiles.FileEntry.Translation(it.key, it.text)
                    }
                    TranslationFiles.addAll(psi, fileEntries.asIterable())
                    EditorNotifications.updateAll()
                }

                if (psi.findMcpModule() == null) {
                    // TranslationSorter.query requires an MCP module to work
                    return@createActionLabel
                }

                val sort = Messages.showYesNoDialog(
                    project,
                    "Would you like to sort all translations now?",
                    "Sort Translations",
                    Messages.getQuestionIcon()
                )
                if (sort == Messages.YES) {
                    TranslationSorter.query(project, psi, Ordering.LIKE_DEFAULT)
                }
            }
            panel.createActionLabel("Hide notification") {
                panel.isVisible = false
                show = false
            }
            return panel
        }
        return null
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
