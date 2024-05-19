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
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.readText
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.textValidation
import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile

class ZipTemplateProvider : TemplateProvider {

    override fun getLabel(): String = "Archive"

    override fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> LoadedTemplate>
    ): JComponent {
        val pathProperty = propertyGraph.property("").apply {
            afterChange { path ->
                provideTemplate.accept {
                    loadTemplateFrom(path)
                }
            }
            bindStorage("${this@ZipTemplateProvider.javaClass.name}.path")
        }

        return panel {
            row(MCDevBundle("creator.ui.custom.path.label")) {
                val pathChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("zip").apply {
                    description = MCDevBundle("creator.ui.custom.archive.dialog.description")
                }
                textFieldWithBrowseButton(
                    MCDevBundle("creator.ui.custom.archive.dialog.title"),
                    context.project,
                    pathChooserDescriptor
                ).align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(pathProperty)
                    .textValidation(validationErrorIf(MCDevBundle("creator.validation.custom.path_not_a_file")) { value ->
                        !Path.of(value).isRegularFile()
                    })
            }
        }
    }

    companion object {

        fun loadTemplateFrom(archivePath: String): ArchiveFileLoadedTemplate {
            val absolutePath = Path.of(archivePath.trim()).absolutePathString()
            val descriptorText = readFromArchive(absolutePath, ".mcdev.template.json")
            val descriptor = Gson().fromJson<TemplateDescriptor>(descriptorText)
            return ArchiveFileLoadedTemplate(absolutePath, descriptor)
        }

        private fun readFromArchive(archivePath: String, innerPath: String): String {
            val fs = JarFileSystem.getInstance()
            val inArchivePath = "$archivePath!/$innerPath"
            val virtualFile = fs.findFileByPath(inArchivePath)
                ?: throw FileNotFoundException("Could not find file $innerPath in archive $archivePath")
            return virtualFile.readText()
        }
    }

    class ArchiveFileLoadedTemplate(
        val archivePath: String,
        override val descriptor: TemplateDescriptor,
    ) : LoadedTemplate {

        override fun loadTemplateContents(path: String): String = readFromArchive(archivePath, path)
    }
}
