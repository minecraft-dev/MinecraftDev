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

package com.demonwav.mcdev.platform.bukkit.creator

import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.ui.validation.WHEN_GRAPH_PROPAGATION_FINISHED
import com.intellij.ui.content.AlertIcon
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected

class SpigotPlatformStep(parent: BukkitPlatformStep) : AbstractBukkitPlatformStep(parent, PlatformType.SPIGOT) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository(
            "spigotmc-repo",
            "https://hub.spigotmc.org/nexus/content/repositories/snapshots/",
        ),
        BuildRepository(
            "sonatype",
            "https://oss.sonatype.org/content/groups/public/",
        ),
    )

    override fun getDependencies(mcVersion: SemanticVersion) = listOf(
        BuildDependency(
            "org.spigotmc",
            "spigot-api",
            "$mcVersion-R0.1-SNAPSHOT",
            mavenScope = "provided",
            gradleConfiguration = "compileOnly",
        ),
    )

    override fun getManifest(): Pair<String, String> {
        return "src/main/resources/plugin.yml" to MinecraftTemplates.BUKKIT_PLUGIN_YML_TEMPLATE
    }

    class Factory : BukkitPlatformStep.Factory {
        override val name = "Spigot"

        override fun createStep(parent: BukkitPlatformStep) = SpigotPlatformStep(parent)
    }
}

class PaperPlatformStep(parent: BukkitPlatformStep) : AbstractBukkitPlatformStep(parent, PlatformType.PAPER) {

    private val usePaperManifestProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.usePaperManifest")

    private val usePaperManifest by usePaperManifestProperty

    override fun setupUI(builder: Panel) {
        super.setupUI(builder)
        with(builder) {
            row("Paper Manifest:") {
                icon(AlertIcon(AllIcons.General.Warning)).comment(
                    "Paper plugins are <a href=\"https://docs.papermc.io/paper/dev/getting-started/paper-plugins\">" +
                        "still experimental</a>, their usage is discouraged for general purpose development. "
                )
                checkBox("Use paper-plugin.yml")
                    .bindSelected(usePaperManifestProperty)
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(propertyGraph))
            }
        }
    }

    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository(
            "papermc-repo",
            "https://repo.papermc.io/repository/maven-public/",
        ),
        BuildRepository(
            "sonatype",
            "https://oss.sonatype.org/content/groups/public/",
        ),
    )

    override fun getDependencies(mcVersion: SemanticVersion): List<BuildDependency> {
        val paperGroupId = when {
            mcVersion >= MinecraftVersions.MC1_17 -> "io.papermc.paper"
            else -> "com.destroystokyo.paper"
        }
        return listOf(
            BuildDependency(
                paperGroupId,
                "paper-api",
                "$mcVersion-R0.1-SNAPSHOT",
                mavenScope = "provided",
                gradleConfiguration = "compileOnly",
            ),
        )
    }

    override fun getManifest(): Pair<String, String> {
        if (usePaperManifest) {
            return "src/main/resources/paper-plugin.yml" to MinecraftTemplates.PAPER_PLUGIN_YML_TEMPLATE
        }

        return "src/main/resources/plugin.yml" to MinecraftTemplates.BUKKIT_PLUGIN_YML_TEMPLATE
    }

    class Factory : BukkitPlatformStep.Factory {
        override val name = "Paper"

        override fun createStep(parent: BukkitPlatformStep) = PaperPlatformStep(parent)
    }
}
