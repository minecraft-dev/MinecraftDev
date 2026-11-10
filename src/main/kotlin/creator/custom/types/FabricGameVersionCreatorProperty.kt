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

package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.CreatorContext
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.util.ui.AsyncProcessIcon
import javax.swing.DefaultComboBoxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A Minecraft version supported by Fabric, without any of the other Fabric versions [FabricVersionsCreatorProperty]
 * provides. Useful for templates delegating the rest of the version selection to an external tool, like Fabric CLI.
 *
 * Only stable versions are offered, plus the latest snapshot if it's newer than every stable version. The latest stable
 * version is selected by default.
 */
class FabricGameVersionCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    context: CreatorContext
) : SemanticVersionCreatorProperty(descriptor, context) {

    private val loadingVersionsProperty = graph.property(true)
    private val versionsModel = DefaultComboBoxModel<SemanticVersion>()

    override fun buildUi(panel: Panel) {
        panel.row(descriptor.translatedLabel) {
            comboBox(versionsModel)
                .bindItem(graphProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .enabled(descriptor.editable != false)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            cell(AsyncProcessIcon(makeStorageKey("progress")))
                .visibleIf(loadingVersionsProperty)
            label(MCDevBundle("creator.ui.versions_download.label"))
                .visibleIf(loadingVersionsProperty)
        }.propertyVisibility()
    }

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        val scope = context.childScope("FabricGameVersionCreatorProperty")
        scope.launch(Dispatchers.Default) {
            val versions = getGameVersions()
            withContext(context.uiContext) {
                versionsModel.removeAllElements()
                versionsModel.addAll(versions.all)
                if (graphProperty.get() !in versions.all) {
                    graphProperty.set(versions.latestStable ?: SemanticVersion(emptyList()))
                }
                // Clearing the model also cleared its selection, which won't be restored by the binding if the
                // property kept its (remembered) value
                versionsModel.selectedItem = graphProperty.get()
                loadingVersionsProperty.set(false)
            }
        }
    }

    companion object {
        private var gameVersions: GameVersions? = null

        private suspend fun getGameVersions(): GameVersions {
            gameVersions?.let { return it }

            val fabricVersions = FabricVersions.downloadData()
                ?: return GameVersions(emptyList(), null)
            val stableVersions = fabricVersions.game.asSequence()
                .filter { it.stable }
                .mapNotNull { SemanticVersion.tryParse(it.version) }
                // Earlier versions aren't supported by Fabric's project generator
                .filter { it >= MinecraftVersions.MC1_14_4 }
                .distinct()
                .toList()

            // Versions are listed newest first, so the latest snapshot is only first if no stable version replaced it
            val latestSnapshot = fabricVersions.game.firstOrNull()
                ?.takeUnless { it.stable }
                ?.let { SemanticVersion.tryParse(it.version) }
            return GameVersions(listOfNotNull(latestSnapshot) + stableVersions, stableVersions.firstOrNull())
                .also { gameVersions = it }
        }
    }

    private data class GameVersions(val all: List<SemanticVersion>, val latestStable: SemanticVersion?)

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            context: CreatorContext
        ): CreatorProperty<*> = FabricGameVersionCreatorProperty(descriptor, context)
    }
}
