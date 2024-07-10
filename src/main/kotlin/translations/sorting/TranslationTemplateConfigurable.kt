/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.translations.sorting

import com.demonwav.mcdev.TranslationSettings
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.translations.lang.colors.LangSyntaxHighlighter
import com.intellij.codeInsight.template.impl.TemplateEditorUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import org.jetbrains.annotations.Nls

class TranslationTemplateConfigurable(private val project: Project) : Configurable {
    private lateinit var cmbScheme: JComboBox<String>
    private var templateEditor: Editor? = null

    private val editorPanel = JPanel(BorderLayout()).apply {
        preferredSize = JBUI.size(250, 350)
        minimumSize = preferredSize
    }

    private val panel = panel {
        row(MCDevBundle("minecraft.settings.lang_template.scheme")) {
            cmbScheme = comboBox(emptyList<String>()).component
        }

        row {
            label(MCDevBundle("minecraft.settings.lang_template.comment"))
        }

        row {
            cell(editorPanel).align(Align.FILL)
        }

        val translationSettings = TranslationSettings.getInstance(project)
        row {
            checkBox(MCDevBundle("minecraft.settings.translation.force_json_translation_file"))
                .bindSelected(translationSettings::isForceJsonTranslationFile)
        }

        lateinit var allowConvertToTranslationTemplate: ComponentPredicate
        row {
            val checkBox = checkBox(MCDevBundle("minecraft.settings.translation.use_custom_convert_template"))
                .bindSelected(translationSettings::isUseCustomConvertToTranslationTemplate)
            allowConvertToTranslationTemplate = checkBox.selected
        }

        row {
            textField().bindText(translationSettings::convertToTranslationTemplate)
                .enabledIf(allowConvertToTranslationTemplate)
                .columns(COLUMNS_LARGE)
        }
    }

    @Nls
    override fun getDisplayName() = MCDevBundle("minecraft.settings.lang_template.display_name")

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JComponent = panel

    private fun getActiveTemplateText() =
        when {
            cmbScheme.selectedIndex == 0 -> TemplateManager.getGlobalTemplateText()
            !project.isDefault -> TemplateManager.getProjectTemplateText(project)
            else -> MCDevBundle("minecraft.settings.lang_template.project_must_be_selected")
        }

    private fun init() {
        if (project.isDefault) {
            cmbScheme.model = DefaultComboBoxModel(arrayOf("Global"))
            cmbScheme.selectedIndex = 0
        } else {
            cmbScheme.model = DefaultComboBoxModel(arrayOf("Global", "Project"))
        }
        cmbScheme.addActionListener {
            setupEditor()
        }

        setupEditor()
    }

    private fun setupEditor() {
        templateEditor = TemplateEditorUtil.createEditor(false, getActiveTemplateText())
        val editorColorsScheme = EditorColorsManager.getInstance().globalScheme
        val highlighter = LexerEditorHighlighter(
            LangSyntaxHighlighter(TranslationTemplateLexerAdapter()),
            editorColorsScheme,
        )
        (templateEditor as EditorEx).highlighter = highlighter
        templateEditor!!.settings.isLineNumbersShown = true

        editorPanel.removeAll()
        editorPanel.add(templateEditor!!.component, BorderLayout.CENTER)
    }

    override fun isModified(): Boolean {
        return templateEditor?.document?.text != getActiveTemplateText() != false || panel.isModified()
    }

    override fun apply() {
        val editor = templateEditor
            ?: return

        val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(panel))
        if (cmbScheme.selectedIndex == 0) {
            TemplateManager.writeGlobalTemplate(editor.document.text)
        } else if (project != null) {
            TemplateManager.writeProjectTemplate(project, editor.document.text)
        }

        panel.apply()
    }

    override fun reset() {
        init()
        panel.reset()
    }
}
