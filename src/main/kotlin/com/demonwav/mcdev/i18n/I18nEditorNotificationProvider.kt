/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.demonwav.mcdev.util.applyWriteAction
import com.demonwav.mcdev.util.mcDomain
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.util.Locale

class I18nEditorNotificationProvider(private val project: Project) : EditorNotifications.Provider<I18nEditorNotificationProvider.InfoPanel>() {
    private var show: Boolean = true

    override fun getKey() = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): InfoPanel? {
        if (!show || file.fileType != I18nFileType || file.nameWithoutExtension.toLowerCase(Locale.ROOT) == I18nConstants.DEFAULT_LOCALE) {
            return null
        }

        val defaultEntries = project.findDefaultLangEntries(scope = Scope.PROJECT, domain = file.mcDomain)
        val entries = project.findLangEntries(file = file, scope = Scope.PROJECT)
        val defaultKeys = defaultEntries.map { it.key }.toMutableSet()
        val keys = entries.map { it.key }
        val entryMap = defaultEntries.associate { it.key to it }

        if (!keys.containsAll(defaultKeys)) {
            val panel = InfoPanel()
            panel.setText("Translation file doesn't match default one (${I18nConstants.DEFAULT_LOCALE}.${I18nConstants.FILE_EXTENSION}).")
            panel.createActionLabel("Add missing default entries (won't reflect changes in original English localization)") {
                val psi = PsiManager.getInstance(project).findFile(file) ?: return@createActionLabel
                psi.applyWriteAction {
                    defaultKeys.removeAll(keys)
                    if (lastChild?.node?.elementType != I18nTypes.LINE_ENDING) {
                        add(I18nElementFactory.createLineEnding(project))
                    }
                    for (key in defaultKeys) {
                        entryMap[key]?.value?.let {
                            add(I18nElementFactory.createEntry(project, key, it))
                            add(I18nElementFactory.createLineEnding(project))
                        }
                    }
                    EditorNotifications.updateAll()
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
