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

import com.intellij.openapi.command.WriteCommandAction
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

    override fun getKey(): Key<InfoPanel> {
        return KEY
    }

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): InfoPanel? {
        if (!show || !file.name.endsWith(".lang") || file.nameWithoutExtension.toLowerCase() == "en_us") {
            return null
        }

        val defaultProperties = project.findDefaultProperties(scope = Scope.PROJECT, domain = I18nElementFactory.getResourceDomain(file))
        val properties = project.findProperties(file = file, scope = Scope.PROJECT)
        val defaultKeys = defaultProperties.map { it?.key }.toMutableSet()
        val keys = properties.map { it?.key }.toMutableSet()
        val propertyMap = defaultProperties.associate { it.key to it }

        if (!keys.containsAll(defaultKeys)) {
            val panel = InfoPanel()
            panel.setText("Translation file doesn't match default one (en_us.lang).")
            panel.createActionLabel("Add missing translations") {
                val psi = PsiManager.getInstance(project).findFile(file)
                object : WriteCommandAction.Simple<Unit>(project, psi) {
                    @Throws(Throwable::class)
                    override fun run() {
                        defaultKeys.removeAll(keys)
                        for (key in defaultKeys) {
                            if (key != null && propertyMap[key]?.value != null && psi != null) {
                                psi.add(I18nElementFactory.createLineEnding(project))
                                psi.add(I18nElementFactory.createProperty(project, key, propertyMap[key]?.value!!))
                            }
                        }
                        EditorNotifications.updateAll()
                    }
                }.execute()
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
