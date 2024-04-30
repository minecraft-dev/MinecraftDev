package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.creator.custom.model.ClassFqn
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class EvaluateTemplateExpressionAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {

        val dialog = EvaluateDialog()
        dialog.isModal = false
        dialog.show()
    }

    private class EvaluateDialog : DialogWrapper(null, false, IdeModalityType.IDE) {
        val document = EditorFactory.getInstance().createDocument("")
        val editor = EditorFactory.getInstance().createEditor(document) as EditorEx

        lateinit var field: JBTextField

        init {
            title = "Evaluate Template Expression"
            isOKActionEnabled = true
            setValidationDelay(0)

            Disposer.register(disposable) {
                EditorFactory.getInstance().releaseEditor(editor)
            }

            init()
        }

        override fun createCenterPanel(): JComponent = panel {
            row {
                cell(editor.component).align(Align.FILL)
            }

            row("Result:") {
                field = textField().align(Align.FILL).component
                field.isEditable = false
            }
        }

        override fun doOKAction() {
            val props = mapOf(
                "BUILD_SYSTEM" to "gradle",
                "USE_PAPER_MANIFEST" to false,
                "MAIN_CLASS" to ClassFqn("io.github.rednesto.test.Test")
            )
            field.text = TemplateEvaluator.evaluate(props, document.text).toString()
        }
    }
}
