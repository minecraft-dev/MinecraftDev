package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.textValidation
import com.intellij.util.io.readText
import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class LocalTemplateProvider : TemplateProvider {

    override fun getLabel(): String = "Local"

    override fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> LoadedTemplate>
    ): JComponent {
        val pathProperty = propertyGraph.property("").apply {
            afterChange { path ->
                provideTemplate.accept {
                    val root = Path.of(path.trim()).absolute()
                    val templateDescriptorPath = root.resolve(".mcdev.template.json")
                    if (!templateDescriptorPath.exists()) {
                        throw FileNotFoundException("Could not find template descriptor at ${templateDescriptorPath.absolutePathString()}")
                    }

                    val descriptor = Gson().fromJson<TemplateDescriptor>(templateDescriptorPath.readText())
                    FileLoadedTemplate(root, descriptor)
                }
            }
            bindStorage("${this@LocalTemplateProvider.javaClass.name}.path")
        }

        return panel {
            row(MCDevBundle("creator.ui.custom.path.label")) {
                val pathChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                    description = MCDevBundle("creator.ui.custom.path.dialog.description")
                }
                textFieldWithBrowseButton(
                    MCDevBundle("creator.ui.custom.path.dialog.title"),
                    context.project,
                    pathChooserDescriptor
                ).align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(pathProperty)
                    .textValidation(validationErrorIf(MCDevBundle("creator.validation.custom.path_not_a_directory")) { value ->
                        val file = kotlin.runCatching {
                            VirtualFileManager.getInstance().findFileByNioPath(Path.of(value))
                        }.getOrNull()
                        file == null || !file.isDirectory
                    })
            }
        }
    }

    private class FileLoadedTemplate(
        val root: Path,
        override val descriptor: TemplateDescriptor,
    ) : LoadedTemplate {

        override fun loadTemplateContents(path: String): String {
            val templatePath = root.resolve(path).toAbsolutePath()
            if (!templatePath.startsWith(root)) {
                throw Exception("Template file path is outside of template root directory")
            }

            return templatePath.readText()
        }
    }
}
