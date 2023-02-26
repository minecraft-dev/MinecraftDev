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
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion

class SpigotPlatformStep(parent: BukkitPlatformStep) : AbstractBukkitPlatformStep(parent, PlatformType.SPIGOT) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository(
            "spigotmc-repo",
            "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"
        ),
        BuildRepository(
            "sonatype",
            "https://oss.sonatype.org/content/groups/public/"
        ),
    )

    override fun getDependencies(mcVersion: SemanticVersion) = listOf(
        BuildDependency(
            "org.spigotmc",
            "spigot-api",
            "$mcVersion-R0.1-SNAPSHOT",
            mavenScope = "provided",
            gradleConfiguration = "compileOnly",
        )
    )

    class Factory : BukkitPlatformStep.Factory {
        override val name = "Spigot"

        override fun createStep(parent: BukkitPlatformStep) = SpigotPlatformStep(parent)
    }
}

class PaperPlatformStep(parent: BukkitPlatformStep) : AbstractBukkitPlatformStep(parent, PlatformType.PAPER) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository(
            "papermc-repo",
            "https://repo.papermc.io/repository/maven-public/"
        ),
        BuildRepository(
            "sonatype",
            "https://oss.sonatype.org/content/groups/public/"
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
                gradleConfiguration = "compileOnly"
            )
        )
    }

    class Factory : BukkitPlatformStep.Factory {
        override val name = "Paper"

        override fun createStep(parent: BukkitPlatformStep) = PaperPlatformStep(parent)
    }
}
