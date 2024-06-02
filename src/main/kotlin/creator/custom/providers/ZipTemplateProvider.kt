package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.textValidation
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.io.path.isRegularFile

class ZipTemplateProvider : TemplateProvider {

    override fun getLabel(): String = "Archive"

    override fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> Collection<LoadedTemplate>>
    ): JComponent {
        val pathProperty = propertyGraph.property("").apply {
            afterChange { path ->
                provideTemplate.accept {
                    loadTemplatesFrom(path)
                }
            }
            bindStorage("${this@ZipTemplateProvider.javaClass.name}.path")
        }

        return panel {
            row(MCDevBundle("creator.ui.custom.path.label")) {
                val pathChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
                    .withFileFilter { it.extension == "zip" }
                    .apply { description = MCDevBundle("creator.ui.custom.archive.dialog.description") }
                textFieldWithBrowseButton(
                    MCDevBundle("creator.ui.custom.archive.dialog.title"),
                    context.project,
                    pathChooserDescriptor
                ).align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(pathProperty)
                    .textValidation(
                        validationErrorIf(MCDevBundle("creator.validation.custom.path_not_a_file")) { value ->
                            runCatching { !Path.of(value).isRegularFile() }.getOrDefault(true)
                        }
                    )
            }
        }
    }

    override fun deserializeAndLoad(element: String): LoadedTemplate? = TemplateProvider.deserializeAndLoadVfs(element)

    companion object {

        fun loadTemplatesFrom(archivePath: String): List<LoadedTemplate> {
            val archiveRoot = archivePath + JarFileSystem.JAR_SEPARATOR
            val fs = JarFileSystem.getInstance()
            val rootFile = fs.refreshAndFindFileByPath(archiveRoot)
                ?: return emptyList()
            return TemplateProvider.findTemplates(rootFile)
        }
    }
}
