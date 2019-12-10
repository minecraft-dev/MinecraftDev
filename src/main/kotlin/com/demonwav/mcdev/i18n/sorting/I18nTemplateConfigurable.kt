/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.sorting

import com.demonwav.mcdev.i18n.lang.colors.I18nSyntaxHighlighter
import com.intellij.codeInsight.template.impl.TemplateEditorUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import org.jetbrains.annotations.Nls

class I18nTemplateConfigurable : Configurable {
    private lateinit var panel: JPanel
    private lateinit var cmbScheme: JComboBox<String>
    private lateinit var editorPanel: JPanel
    private lateinit var templateEditor: Editor

    @Nls
    override fun getDisplayName() = "Localization Template"

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JComponent {
        return panel
    }

    private fun getActiveTemplateText(project: Project?) =
        when {
            cmbScheme.selectedIndex == 0 -> TemplateManager.getGlobalTemplateText()
            project != null -> TemplateManager.getProjectTemplateText(project)
            else -> "You must have selected a project for this!"
        }

    private fun init() {
        val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(panel))
        if (project == null) {
            cmbScheme.selectedIndex = 0
            cmbScheme.model = DefaultComboBoxModel(arrayOf("Global"))
        } else if (cmbScheme.selectedIndex == 0) {
            cmbScheme.model = DefaultComboBoxModel(arrayOf("Global", "Project"))
        }
        cmbScheme.addActionListener {
            setupEditor(project)
        }

        setupEditor(project)
    }

    private fun setupEditor(project: Project?) {
        templateEditor = TemplateEditorUtil.createEditor(false, getActiveTemplateText(project))
        val editorColorsScheme = EditorColorsManager.getInstance().globalScheme
        val highlighter = LexerEditorHighlighter(I18nSyntaxHighlighter(I18nTemplateLexerAdapter()), editorColorsScheme)
        (templateEditor as EditorEx).highlighter = highlighter
        templateEditor.settings.isLineNumbersShown = true

        editorPanel.preferredSize = JBUI.size(250, 100)
        editorPanel.minimumSize = editorPanel.preferredSize
        editorPanel.removeAll()
        editorPanel.add(templateEditor.component, BorderLayout.CENTER)
    }

    override fun isModified(): Boolean {
        return templateEditor.document.text != getActiveTemplateText(
            CommonDataKeys.PROJECT.getData(
                DataManager.getInstance().getDataContext(
                    panel
                )
            )
        )
    }

    override fun apply() {
        val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(panel))
        if (cmbScheme.selectedIndex == 0) {
            TemplateManager.writeGlobalTemplate(templateEditor.document.text)
        } else if (project != null) {
            TemplateManager.writeProjectTemplate(project, templateEditor.document.text)
        }
    }

    override fun reset() {
        init()
    }
}
