/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.demonwav.mcdev.i18n.sorting.I18nSorter
import com.demonwav.mcdev.i18n.sorting.Ordering
import com.demonwav.mcdev.util.applyWriteAction
import com.demonwav.mcdev.util.mcDomain
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.util.Locale

class I18nEditorNotificationProvider(private val project: Project) :
    EditorNotifications.Provider<I18nEditorNotificationProvider.InfoPanel>() {
    private var show: Boolean = true

    override fun getKey() = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): InfoPanel? {
        if (
            !show || file.fileType != I18nFileType ||
            file.nameWithoutExtension.toLowerCase(Locale.ROOT) == I18nConstants.DEFAULT_LOCALE
        ) {
            return null
        }

        if (getMissingEntries(file).isNotEmpty()) {
            val panel = InfoPanel()
            panel.setText("Translation file doesn't match default one (${I18nConstants.DEFAULT_LOCALE_FILE}).")
            panel.createActionLabel(
                "Add missing default entries (won't reflect changes in original English localization)"
            ) {
                val psi = PsiManager.getInstance(project).findFile(file) ?: return@createActionLabel
                val missingEntries = getMissingEntries(file)
                psi.applyWriteAction {
                    if (lastChild != null) {
                        if (lastChild?.node?.elementType != I18nTypes.LINE_ENDING) {
                            add(I18nElementFactory.createLineEnding(project))
                        }
                        add(I18nElementFactory.createLineEnding(project))
                    }
                    val newElements = I18nElementFactory.assembleElements(project, missingEntries.values, Int.MAX_VALUE)
                    for (element in newElements) {
                        add(element)
                    }
                    EditorNotifications.updateAll()
                }
                val sort = Messages.showYesNoDialog(
                    project,
                    "Would you like to sort all translations now?",
                    "Sort Translations",
                    Messages.getQuestionIcon()
                )
                if (sort == Messages.YES) {
                    I18nSorter.query(project, psi, Ordering.LIKE_DEFAULT)
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

    private fun getMissingEntries(file: VirtualFile): Map<String, I18nEntry> {
        val defaultEntries = project.findDefaultLangEntries(scope = Scope.PROJECT, domain = file.mcDomain)
        val entries = project.findLangEntries(file = file, scope = Scope.PROJECT)
        val keys = entries.map { it.key }
        val missingEntries = defaultEntries.associateBy { it.key }.toMutableMap()
        missingEntries.keys.removeAll(keys)
        return missingEntries
    }

    class InfoPanel : EditorNotificationPanel() {
        override fun getBackground(): Color {
            val color = EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.NOTIFICATION_BACKGROUND)
            return color ?: UIUtil.getPanelBackground()
        }
    }

    companion object {
        private val KEY = Key.create<InfoPanel>("minecraft.editors.translations")
    }
}
