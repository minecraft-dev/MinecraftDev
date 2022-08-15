/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.version

import com.demonwav.mcdev.util.SemanticVersion
import com.extracraftx.minecraft.templatemakerfabric.data.DataProvider
import com.extracraftx.minecraft.templatemakerfabric.data.holders.LoaderVersion
import java.io.IOException

class FabricVersion private constructor(val versions: DataProvider) {

    fun getFabricVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        val versionText = mcVersion.toString()
        return versions.getFilteredLoaderVersions(
            versions.getDefaultLoomVersion(
                versions.getFilteredYarnVersions(
                    versions.getNormalizedMinecraftVersion(versionText)
                ).firstOrNull()
            )
        )
            .map { loaderVersion: LoaderVersion ->
                SemanticVersion.parse(loaderVersion.toString())
            }
            .sortedDescending()
            .take(50)
            .toList()
    }

    fun getFabricApiVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        val versionText = mcVersion.toString()
        return versions.sortedFabricApiVersions.filter { indexedFabricApiVersion ->
            indexedFabricApiVersion.mcVersion.equals(
                versionText
            )
        }.map { indexedFabricApiVersion -> SemanticVersion.parse(indexedFabricApiVersion.mavenVersion) }
            .sortedDescending().take(50)
    }

    companion object {
        private val dataProvider: DataProvider = DataProvider()

        fun downloadData(): FabricVersion? {
            try {
                dataProvider.loaderVersions
                dataProvider.loomVersions
                dataProvider.yarnVersions
                dataProvider.fabricApiVersions
                return FabricVersion(dataProvider)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }
}
