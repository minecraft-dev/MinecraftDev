/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.creator

import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.ui.validation.AFTER_GRAPH_PROPAGATION
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
            row("Paper manifest:") {
                checkBox("Use paper-plugin.yml")
                    .bindSelected(usePaperManifestProperty)
                    .validationRequestor(AFTER_GRAPH_PROPAGATION(propertyGraph))
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

class FoliaPlatformStep(parent: BukkitPlatformStep) : AbstractBukkitPlatformStep(parent, PlatformType.FOLIA) {

    private val usePaperManifestProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.usePaperManifest")

    private val usePaperManifest by usePaperManifestProperty

    override fun setupUI(builder: Panel) {
        super.setupUI(builder)
        with(builder) {
            row("Paper manifest:") {
                checkBox("Use paper-plugin.yml")
                    .bindSelected(usePaperManifestProperty)
                    .validationRequestor(AFTER_GRAPH_PROPAGATION(propertyGraph))
            }
        }
    }

    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository(
            "papermc-repo",
            "https://repo.papermc.io/repository/maven-public/",
        ),
    )

    override fun getDependencies(mcVersion: SemanticVersion): List<BuildDependency> {
        return listOf(
            BuildDependency(
                "dev.folia",
                "folia-api",
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
        override val name = "Folia"

        override fun createStep(parent: BukkitPlatformStep) = FoliaPlatformStep(parent)
    }
}
