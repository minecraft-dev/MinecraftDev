/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.demonwav.mcdev.util.SemanticVersion.Companion.VersionPart.PreReleasePart
import com.demonwav.mcdev.util.SemanticVersion.Companion.VersionPart.ReleasePart

/**
 * Represents a comparable and generalised "semantic version".
 * Each constituent part (delimited by periods in a version string) contributes
 * to the version ranking with decreasing priority from left to right.
 */
class SemanticVersion(val parts: List<VersionPart>) : Comparable<SemanticVersion> {
    val versionString = parts.joinToString(".") { it.versionString }

    override fun compareTo(other: SemanticVersion) =
        naturalOrder<VersionPart>().lexicographical().compare(parts, other.parts)

    override fun equals(other: Any?) =
        when (other) {
            is SemanticVersion -> parts.size == other.parts.size && parts.zip(other.parts).all { (a, b) -> a == b }
            else -> false
        }

    override fun hashCode() = parts.hashCode()

    companion object {
        /**
         * Parses a version string into a comparable representation.
         * @throws IllegalArgumentException if any part of the version string cannot be parsed as integer or split into pre-release parts.
         */
        fun parse(value: String): SemanticVersion {
            fun parseInt(part: String): Int =
                if (part.all { it.isDigit() })
                    part.toInt()
                else
                    throw IllegalArgumentException("Failed to parse version part as integer: $part")

            val parts = value.split('.').map {
                if (it.contains("_pre")) {
                    // There have been cases of Forge builds for MC pre-releases (1.7.10_pre4)
                    // We're consuming the 10_pre4 and extracting 10 and 4 from it
                    val subParts = it.split("_pre")
                    if (subParts.size == 2) {
                        PreReleasePart(parseInt(subParts[0]), parseInt(subParts[1]))
                    } else {
                        throw IllegalArgumentException("Failed to split pre-release version part into two numbers: $it")
                    }
                } else {
                    ReleasePart(parseInt(it))
                }
            }
            return SemanticVersion(parts)
        }

        sealed class VersionPart : Comparable<VersionPart> {
            abstract val versionString: String

            data class ReleasePart(val version: Int) : VersionPart() {
                override val versionString = version.toString()

                override fun compareTo(other: VersionPart) =
                    when (other) {
                        is PreReleasePart -> if (version != other.version) version - other.version else 1
                        is ReleasePart -> version - other.version
                    }
            }

            data class PreReleasePart(val version: Int, val pre: Int) : VersionPart() {
                override val versionString = "${version}_pre$pre"

                override fun compareTo(other: VersionPart) =
                    when (other) {
                        is PreReleasePart -> if (version != other.version) version - other.version else pre - other.pre
                        is ReleasePart -> if (version != other.version) version - other.version else -1
                    }
            }
        }
    }
}
