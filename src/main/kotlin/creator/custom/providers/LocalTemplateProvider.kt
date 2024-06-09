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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.modalityState
import com.demonwav.mcdev.util.virtualFile
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.ModalityState
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
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.io.path.absolute

class LocalTemplateProvider : TemplateProvider {

    override fun getLabel(): String = "Local"

    override fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> Collection<LoadedTemplate>>
    ): JComponent {
        val pathProperty = propertyGraph.property("").apply {
            afterChange { path ->
                provideTemplate.accept {
                    val rootPath = Path.of(path.trim()).absolute()
                    rootPath.virtualFile?.let { TemplateProvider.findTemplates(context.modalityState, it) }
                        ?: emptyList()
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
                    .textValidation(
                        validationErrorIf(MCDevBundle("creator.validation.custom.path_not_a_directory")) { value ->
                            val file = kotlin.runCatching {
                                VirtualFileManager.getInstance().findFileByNioPath(Path.of(value))
                            }.getOrNull()
                            file == null || !file.isDirectory
                        }
                    )
            }
        }
    }

    override fun deserializeAndLoad(element: String, modalityState: ModalityState): LoadedTemplate? =
        TemplateProvider.deserializeAndLoadVfs(element, modalityState)
}
