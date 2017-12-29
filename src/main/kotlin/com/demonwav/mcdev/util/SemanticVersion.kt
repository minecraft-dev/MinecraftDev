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
import com.demonwav.mcdev.util.SemanticVersion.Companion.VersionPart.TextPart

/**
 * Represents a comparable and generalised "semantic version".
 * Each constituent part (delimited by periods in a version string) contributes
 * to the version ranking with decreasing priority from left to right.
 */
class SemanticVersion(val parts: List<VersionPart>) : Comparable<SemanticVersion> {
    val versionString = parts.joinToString(".") { it.versionString }

    override fun compareTo(other: SemanticVersion): Int {
        // Zipping limits the compared parts to the shorter version, then we perform a component-wise comparison
        // Short-circuits if any component of this version is smaller/older
        val result = parts.zip(other.parts).fold(0) { acc, (a, b) -> if (acc != 0) acc else a.compareTo(b) }
        // When all the parts are equal, the longer version wins
        // Generally speaking, 1.0 is considered older than 1.0.1 (see MC 1.12 vs 1.12.1)
        if (parts.size != other.parts.size && result == 0) {
            return parts.size - other.parts.size
        }
        return result
    }

    override fun equals(other: Any?) =
        when (other) {
            is SemanticVersion -> parts.size == other.parts.size && parts.zip(other.parts).all { (a, b) -> a == b }
            else -> false
        }

    override fun hashCode() = parts.hashCode()

    companion object {
        /**
         * The higher the priority value associated with a certain part, the higher it is ranked in comparisons.
         * Unknown parts will always be "rated" lower and equal among each other.
         */
        val TEXT_PRIORITIES = mapOf(
            "snapshot" to 0,
            "rc" to 1
        )

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
                } else if (it.contains("-")) {
                    // We might consume snapshot versions as well
                    // Those are generally rated the lowest in comparisons
                    // As a simplification, treat anything separated by a dash like that
                    val subParts = it.split("-", limit = 2)
                    TextPart(parseInt(subParts[0]), subParts[1])
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
                        is ReleasePart -> version - other.version
                        is PreReleasePart -> if (version != other.version) version - other.version else 1
                        is TextPart -> if (version != other.version) version - other.version else 1
                    }
            }

            data class PreReleasePart(val version: Int, val pre: Int) : VersionPart() {
                override val versionString = "${version}_pre$pre"

                override fun compareTo(other: VersionPart) =
                    when (other) {
                        is ReleasePart -> if (version != other.version) version - other.version else -1
                        is PreReleasePart -> if (version != other.version) version - other.version else pre - other.pre
                        is TextPart -> if (version != other.version) version - other.version else 1
                    }
            }

            data class TextPart(val version: Int, val text: String) : VersionPart() {
                private val priority = TEXT_PRIORITIES[text.toLowerCase()] ?: -1

                override val versionString = "$version-$text"

                override fun compareTo(other: VersionPart) =
                    when (other) {
                        is ReleasePart -> if (version != other.version) version - other.version else -1
                        is PreReleasePart -> if (version != other.version) version - other.version else -1
                        is TextPart -> if (version != other.version) version - other.version else priority - other.priority
                    }
            }
        }
    }
}
