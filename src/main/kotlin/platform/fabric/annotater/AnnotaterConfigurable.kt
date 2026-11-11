/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.fabric.annotater

import com.demonwav.mcdev.asset.GeneralAssets
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.minecraft.MinecraftModuleSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import java.awt.Font
import java.io.IOException
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingConstants
import kotlin.io.path.Path
import org.jetbrains.annotations.Nls

class AnnotaterConfigurable : Configurable {
    private lateinit var panel: DialogPanel

    @Nls
    override fun getDisplayName() = MCDevBundle("fabric.annotater.settings.display_name")

    override fun createComponent(): JComponent = panel {
        row(
            JLabel(
                MCDevBundle("fabric.annotater.settings.title"),
                GeneralAssets.TINY_POTATO_LARGE,
                SwingConstants.LEADING,
            )
                .apply { font = font.deriveFont(Font.BOLD, font.size * 1.5f) },
        ) {}.bottomGap(BottomGap.MEDIUM)

        val settings = AnnotaterSettings.instance
        lateinit var enabled: Cell<JBCheckBox>
        row {
            enabled = checkBox(MCDevBundle("fabric.annotater.settings.enabled"))
                .bindSelected(settings::enabled)
                .comment(MCDevBundle("fabric.annotater.settings.enabled.comment"))
        }
        indent {
            row {
                text(contributionGuidelinesNote())
            }
            row {
                checkBox(MCDevBundle("fabric.annotater.settings.show_javadocs"))
                    .bindSelected(settings::showAnnotaterJavadocs)
                    .comment(MCDevBundle("fabric.annotater.settings.show_javadocs.comment"))
            }
            lateinit var managed: Cell<JBRadioButton>
            lateinit var custom: Cell<JBRadioButton>
            buttonsGroup {
                row {
                    managed = radioButton(
                        MCDevBundle("fabric.annotater.settings.managed"),
                        AnnotaterSettings.CheckoutMode.MANAGED,
                    )
                }
                indent {
                    row(MCDevBundle("fabric.annotater.settings.managed_root")) {
                        textFieldWithBrowseButton(FileChooserDescriptorFactory.singleDir())
                            .bindText(settings::managedRoot)
                            .applyToComponent {
                                (textField as? JBTextField)?.emptyText?.text =
                                    AnnotaterSettings.defaultManagedRoot.toString()
                            }
                            .align(AlignX.FILL)
                            .enabledIf(managed.selected)
                    }
                    row {
                        val vcs = AnnotaterVcs.instance
                        button(MCDevBundle("fabric.annotater.settings.clone_now")) {
                            cloneOrUpdate(vcs ?: return@button)
                        }.enabled(vcs != null).enabledIf(managed.selected)
                        if (vcs == null) {
                            comment(MCDevBundle("fabric.annotater.settings.no_git"))
                        }
                    }
                }
                row {
                    custom = radioButton(
                        MCDevBundle("fabric.annotater.settings.custom"),
                        AnnotaterSettings.CheckoutMode.CUSTOM,
                    )
                }
                indent {
                    row {
                        textFieldWithBrowseButton(FileChooserDescriptorFactory.singleDir())
                            .bindText(settings::customDirectory)
                            .align(AlignX.FILL)
                            .enabledIf(custom.selected)
                            .validationOnInput { field ->
                                val error = validateCustomDirectory(field.text)
                                when {
                                    error == null -> null
                                    error.second -> error(error.first)
                                    else -> warning(error.first)
                                }
                            }
                            .comment(MCDevBundle("fabric.annotater.settings.custom.comment"))
                    }
                }
            }.bind(settings::checkoutMode)
        }.enabledIf(enabled.selected)

        onApply {
            ApplicationManager.getApplication().messageBus.syncPublisher(AnnotaterListener.TOPIC).checkoutsChanged()
        }
    }.also { panel = it }

    /**
     * Returns the problem and whether it's an error rather than a warning.
     */
    private fun validateCustomDirectory(directory: String): Pair<String, Boolean>? {
        if (directory.isBlank()) {
            return null
        }
        val config = try {
            AnnotaterConfig.read(Path(directory))
        } catch (e: IOException) {
            return MCDevBundle("fabric.annotater.status.invalid", e.message ?: e.toString()) to true
        }
        val versions = knownMinecraftVersions()
        if (versions.isNotEmpty() && config.version !in versions) {
            val message = MCDevBundle(
                "fabric.annotater.settings.custom.version_mismatch",
                config.version,
                versions.joinToString(", "),
            )
            return message to false
        }
        return null
    }

    /**
     * The guidelines can differ between version branches, so link to those of each version in use.
     */
    private fun contributionGuidelinesNote(): String {
        fun link(branch: String, text: String) =
            "<a href=\"https://github.com/FabricMC/annotater/blob/$branch/CONTRIBUTING.md\">$text</a>"

        val versions = knownMinecraftVersions()
        val links = when (versions.size) {
            0 -> link("HEAD", MCDevBundle("fabric.annotater.settings.guidelines.link"))
            1 -> link(versions.single(), MCDevBundle("fabric.annotater.settings.guidelines.link"))
            else -> MCDevBundle(
                "fabric.annotater.settings.guidelines.link.version",
                versions.joinToString(", ") { link(it, it) },
            )
        }
        return MCDevBundle("fabric.annotater.settings.guidelines", links)
    }

    private fun cloneOrUpdate(vcs: AnnotaterVcs) {
        val versions = knownMinecraftVersions()
        val version = when (versions.size) {
            0 -> {
                Messages.showWarningDialog(
                    panel,
                    MCDevBundle("fabric.annotater.settings.clone_now.no_versions"),
                    MCDevBundle("fabric.annotater.settings.clone_now"),
                )
                return
            }

            1 -> versions.single()
            else -> Messages.showEditableChooseDialog(
                MCDevBundle("fabric.annotater.settings.clone_now.version"),
                MCDevBundle("fabric.annotater.settings.clone_now"),
                null,
                versions.toTypedArray(),
                versions.first(),
                null,
            )?.takeIf { it.isNotBlank() } ?: return
        }

        // Clone into the directory currently in the form, even if it hasn't been applied yet
        panel.apply()
        vcs.cloneOrUpdate(null, version, modal = true) { result ->
            result.onSuccess { checkout ->
                Messages.showInfoMessage(
                    panel,
                    MCDevBundle("fabric.annotater.cloned.content", checkout.root),
                    MCDevBundle("fabric.annotater.cloned", version),
                )
            }.onFailure { error ->
                Messages.showErrorDialog(
                    panel,
                    error.message ?: error.toString(),
                    MCDevBundle("fabric.annotater.clone_failed"),
                )
            }
        }
    }

    override fun isModified(): Boolean = panel.isModified()

    override fun apply() = panel.apply()

    override fun reset() = panel.reset()

    private fun knownMinecraftVersions(): List<String> {
        return ProjectManager.getInstance().openProjects.flatMap { project ->
            ModuleManager.getInstance(project).modules.mapNotNull {
                MinecraftModuleSettings.getInstance(it).state.minecraftVersion
            }
        }.filter { it.isNotBlank() }.distinct()
    }

    object Util {
        fun show(project: Project?) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AnnotaterConfigurable::class.java)
        }
    }
}
