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
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.modalityState
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.refreshSync
import com.demonwav.mcdev.util.virtualFile
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class BuiltinTemplateProvider : RemoteTemplateProvider() {

    private val builtinRepoUrl = "https://github.com/minecraft-dev/templates/archive/refs/heads/v\$version.zip"
    private val builtinTemplatesPath = PluginUtil.plugin.pluginPath.resolve("lib/resources/builtin-templates")
    private val builtinTemplatesInnerPath = "templates-${TemplateDescriptor.FORMAT_VERSION}"
    private var repoUpdated: Boolean = false

    override val label: String = MCDevBundle("template.provider.builtin.label")

    override val hasConfig: Boolean = true

    override fun init(indicator: ProgressIndicator, repos: List<MinecraftSettings.TemplateRepo>) {
        if (repoUpdated || repos.none { it.data.toBoolean() }) {
            // Auto update is disabled
            return
        }

        if (doUpdateRepo(indicator, label, builtinRepoUrl)) {
            repoUpdated = true
        }
    }

    override fun loadTemplates(
        context: WizardContext,
        repo: MinecraftSettings.TemplateRepo
    ): Collection<LoadedTemplate> {
        val remoteTemplates = doLoadTemplates(context, repo, builtinTemplatesInnerPath)
        if (remoteTemplates.isNotEmpty()) {
            return remoteTemplates
        }

        val repoRoot = builtinTemplatesPath.virtualFile
            ?: return emptyList()
        repoRoot.refreshSync(context.modalityState)
        return TemplateProvider.findTemplates(context.modalityState, repoRoot)
    }

    override fun setupConfigUi(
        data: String,
        dataSetter: (String) -> Unit
    ): JComponent? {
        val propertyGraph = PropertyGraph("BuiltinTemplateProvider config")
        val autoUpdateProperty = propertyGraph.property(data.toBooleanStrictOrNull() != false)

        return panel {
            row {
                checkBox(MCDevBundle("creator.ui.custom.remote.auto_update.label"))
                    .bindSelected(autoUpdateProperty)
            }

            onApply {
                dataSetter(autoUpdateProperty.get().toString())
            }
        }
    }
}
