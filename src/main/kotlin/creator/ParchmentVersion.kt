package com.demonwav.mcdev.creator

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.intellij.openapi.diagnostic.logger

class ParchmentVersion private constructor(val versions: List<String>) {
    companion object {
        private val LOGGER = logger<ParchmentVersion>()

        suspend fun downloadData(mcVersion: String): ParchmentVersion? {
            try {
                // Using this URL doesn't work currently
                // val url = "https://maven.parchmentmc.org/org/parchmentmc/parchment-$mcVersion/maven-metadata.xml"
                val url = "https://ldtteam.jfrog.io/artifactory/parchmentmc-public/org/parchmentmc/data/parchment-$mcVersion/maven-metadata.xml"
                val versions = collectMavenVersions(url)
                return ParchmentVersion(versions)
            } catch (e: Exception) {
                if (e is FuelError && e.exception is HttpException) {
                    return null
                }
                LOGGER.error("Failed to retrieve Parchment $mcVersion version data", e)
            }
            return null
        }
    }
}
