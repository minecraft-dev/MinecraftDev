/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.creator

import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.SemanticVersion

class BungeeMainPlatformStep(parent: BungeePlatformStep) : AbstractBungeePlatformStep(parent, PlatformType.BUNGEECORD) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository("sonatype", "https://oss.sonatype.org/content/groups/public/")
    )

    override fun getDependencies(mcVersion: SemanticVersion) = listOf(
        BuildDependency(
            "net.md-5",
            "bungeecord-api",
            mcVersion.toString(),
            mavenScope = "provided",
            gradleConfiguration = "compileOnly"
        )
    )

    class Factory : BungeePlatformStep.Factory {
        override val name = "BungeeCord"

        override fun createStep(parent: BungeePlatformStep) = BungeeMainPlatformStep(parent)
    }
}

class WaterfallPlatformStep(parent: BungeePlatformStep) : AbstractBungeePlatformStep(parent, PlatformType.WATERFALL) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository("sonatype", "https://oss.sonatype.org/content/groups/public/"),
        BuildRepository(
            "papermc-repo",
            "https://repo.papermc.io/repository/maven-public/"
        ),
    )

    override fun getDependencies(mcVersion: SemanticVersion) = listOf(
        BuildDependency(
            "io.github.waterfallmc",
            "waterfall-api",
            "$mcVersion-SNAPSHOT",
            mavenScope = "provided",
            gradleConfiguration = "compileOnly"
        ),
    )

    class Factory : BungeePlatformStep.Factory {
        override val name = "Waterfall"

        override fun createStep(parent: BungeePlatformStep) = WaterfallPlatformStep(parent)
    }
}
