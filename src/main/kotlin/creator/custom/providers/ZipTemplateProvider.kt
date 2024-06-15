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

package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.modalityState
import com.demonwav.mcdev.util.refreshSync
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.ui.validation.validationErrorIf
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.textValidation
import java.nio.file.Path
import javax.swing.JComponent
import kotlin.io.path.isRegularFile

class ZipTemplateProvider : TemplateProvider {

    override val label: String = MCDevBundle("template.provider.zip.label")

    override val hasConfig: Boolean = true

    override fun loadTemplates(
        context: WizardContext,
        repo: MinecraftSettings.TemplateRepo
    ): Collection<LoadedTemplate> {
        val archiveRoot = repo.data + JarFileSystem.JAR_SEPARATOR
        val fs = JarFileSystem.getInstance()
        val rootFile = fs.refreshAndFindFileByPath(archiveRoot)
            ?: return emptyList()
        val modalityState = context.modalityState
        rootFile.refreshSync(modalityState)
        return TemplateProvider.findTemplates(modalityState, rootFile)
    }

    override fun setupConfigUi(
        data: String,
        dataSetter: (String) -> Unit
    ): JComponent {
        val propertyGraph = PropertyGraph("ZipTemplateProvider config")
        val pathProperty = propertyGraph.property(data)

        return panel {
            row(MCDevBundle("creator.ui.custom.path.label")) {
                val pathChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
                    .withFileFilter { it.extension == "zip" }
                    .apply { description = MCDevBundle("creator.ui.custom.archive.dialog.description") }
                textFieldWithBrowseButton(
                    MCDevBundle("creator.ui.custom.archive.dialog.title"),
                    null,
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

            onApply {
                dataSetter(pathProperty.get())
            }
        }
    }
}
