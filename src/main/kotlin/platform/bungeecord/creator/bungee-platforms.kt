/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.bungeecord.creator

import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.SemanticVersion

class BungeeMainPlatformStep(parent: BungeePlatformStep) : AbstractBungeePlatformStep(parent, PlatformType.BUNGEECORD) {
    override fun getRepositories(mcVersion: SemanticVersion) = listOf(
        BuildRepository("sonatype", "https://oss.sonatype.org/content/groups/public/"),
    )

    override fun getDependencies(mcVersion: SemanticVersion) = listOf(
        BuildDependency(
            "net.md-5",
            "bungeecord-api",
            mcVersion.toString(),
            mavenScope = "provided",
            gradleConfiguration = "compileOnly",
        ),
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
            "https://repo.papermc.io/repository/maven-public/",
        ),
    )

    override fun getDependencies(mcVersion: SemanticVersion) = listOf(
        BuildDependency(
            "io.github.waterfallmc",
            "waterfall-api",
            "$mcVersion-SNAPSHOT",
            mavenScope = "provided",
            gradleConfiguration = "compileOnly",
        ),
    )

    class Factory : BungeePlatformStep.Factory {
        override val name = "Waterfall"

        override fun createStep(parent: BungeePlatformStep) = WaterfallPlatformStep(parent)
    }
}
