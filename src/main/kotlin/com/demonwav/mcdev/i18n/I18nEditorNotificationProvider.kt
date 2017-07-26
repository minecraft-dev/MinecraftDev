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
import com.demonwav.mcdev.util.runWriteAction
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

class I18nEditorNotificationProvider(private val project: Project) : EditorNotifications.Provider<I18nEditorNotificationProvider.InfoPanel>() {
    private var show: Boolean = true

    override fun getKey() = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): InfoPanel? {
        if (!show || file.fileType != I18nFileType || file.nameWithoutExtension.toLowerCase() == I18nConstants.DEFAULT_LOCALE) {
            return null
        }

        val defaultProperties = project.findDefaultProperties(scope = Scope.PROJECT, domain = I18nElementFactory.getResourceDomain(file))
        val properties = project.findProperties(file = file, scope = Scope.PROJECT)
        val defaultKeys = defaultProperties.map { it.key }.toMutableSet()
        val keys = properties.map { it.key }
        val propertyMap = defaultProperties.associate { it.key to it }

        if (!keys.containsAll(defaultKeys)) {
            val panel = InfoPanel()
            panel.setText("Translation file doesn't match default one (${I18nConstants.DEFAULT_LOCALE}.lang).")
            panel.createActionLabel("Add missing default entries (won't reflect changes in original English localization)") {
                val psi = PsiManager.getInstance(project).findFile(file) ?: return@createActionLabel
                psi.runWriteAction {
                    defaultKeys.removeAll(keys)
                    if (lastChild?.node?.elementType != I18nTypes.LINE_ENDING) {
                        add(I18nElementFactory.createLineEnding(project))
                    }
                    for (key in defaultKeys) {
                        if (propertyMap[key]?.value != null) {
                            add(I18nElementFactory.createProperty(project, key, propertyMap[key]!!.value))
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
