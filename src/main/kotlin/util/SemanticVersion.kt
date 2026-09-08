/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.creator.custom.model.TemplateApi
import com.demonwav.mcdev.util.SemanticVersion.Companion.VersionPart.PreReleasePart
import com.demonwav.mcdev.util.SemanticVersion.Companion.VersionPart.ReleasePart
import com.demonwav.mcdev.util.SemanticVersion.Companion.VersionPart.TextPart
import java.net.URLDecoder

/**
 * Represents a comparable and generalised "semantic version".
 * Each constituent part (delimited by periods in a version string) contributes
 * to the version ranking with decreasing priority from left to right.
 */
@TemplateApi
class SemanticVersion(
    val parts: List<VersionPart>,
    private val buildMetadata: String = "",
) : Comparable<SemanticVersion> {

    private fun createVersionString(): String {
        val mainPart = parts.joinToString(".") { it.versionString }
        return if (buildMetadata.isBlank()) {
            mainPart
        } else {
            "$mainPart+$buildMetadata"
        }
    }

    private val versionString = createVersionString()

    override fun compareTo(other: SemanticVersion): Int =
        naturalOrder<VersionPart>().lexicographical().compare(parts, other.parts)

    override fun equals(other: Any?) =
        when (other) {
            is SemanticVersion -> parts.size == other.parts.size && parts.zip(other.parts).all { (a, b) -> a == b }
            else -> false
        }

    override fun hashCode() = parts.hashCode()

    override fun toString() = versionString

    fun take(count: Int): SemanticVersion {
        return SemanticVersion(parts.take(count))
    }

    fun startsWith(other: SemanticVersion): Boolean {
        if (other.parts.size > this.parts.size) {
            return false
        }
        for (i in other.parts.indices) {
            if (this.parts[i] != other.parts[i]) {
                return false
            }
        }
        return true
    }

    companion object {
        /**
         * The higher the priority value associated with a certain part, the higher it is ranked in comparisons.
         * Unknown parts will always be "rated" lower and equal among each other.
         */
        val TEXT_PRIORITIES = mapOf(
            "snapshot" to 0,
            "rc" to 1,
            "pre" to 1,
        )

        /**
         * All separators allowed between a number and a modifier (i.e. (numbered) text part).
         */
        private val SEPARATORS = listOf('-', '_')

        /**
         * Creates a simple release version where each provided value forms a part (read from left to right).
         */
        fun release(vararg parts: Int) = SemanticVersion(parts.map { ReleasePart(it, it.toString()) })

        fun tryParse(value: String): SemanticVersion? {
            return try {
                parse(value)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        /**
         * Parses a version string into a comparable representation.
         * @throws IllegalArgumentException if any part of the version string cannot be parsed as integer or split into text parts.
         */
        fun parse(value: String): SemanticVersion {
            fun parseInt(part: String): Int =
                if (part.all { it.isDigit() }) {
                    part.toInt()
                } else {
                    throw IllegalArgumentException(
                        "Failed to parse version part as integer: $part " +
                            "(whole version text: $value)",
                    )
                }

            // We need to support pre-releases/RCs and snapshots as well
            fun parsePreReleasePart(
                versionPart: String,
                preReleasePart: String,
                separator: Char,
                versionString: String,
            ): VersionPart {
                val version = parseInt(versionPart)
                if (!preReleasePart.contains('.')) {
                    // support the case where pre-releases etc aren't separated by a dot
                    val (text, number) = preReleasePart.span { !it.isDigit() }
                    val subParts = when {
                        text.isEmpty() -> listOf(ReleasePart(parseInt(number), number))
                        number.isEmpty() -> listOf(TextPart(text))
                        else -> listOf(TextPart(text), ReleasePart(parseInt(number), number))
                    }
                    return PreReleasePart(version, separator, subParts, versionString)
                } else {
                    val subParts = preReleasePart.split(".").map { subPart ->
                        val number = subPart.toIntOrNull()
                        if (number == null) {
                            TextPart(subPart)
                        } else {
                            ReleasePart(number, subPart)
                        }
                    }
                    return PreReleasePart(version, separator, subParts, versionString)
                }
            }

            // Regular Minecraft snapshot versions e.g. 24w39a
            fun parseMinecraftSnapshot(value: String): SemanticVersion? {
                if (value.length != 6 || value[2] != 'w' || !value[5].isLetter()) {
                    return null
                }

                val shortYear = value.substring(0, 2).toIntOrNull() ?: return null
                val week = value.substring(3, 5).toIntOrNull() ?: return null

                val subParts = listOf(ReleasePart(week, week.toString()), TextPart(value[5].toString()))
                val mainPart = PreReleasePart(shortYear, 'w', subParts, value)
                return SemanticVersion(
                    listOf(mainPart),
                )
            }

            parseMinecraftSnapshot(value)?.let { return it }

            val decodedValue = value.split('+').joinToString("+") { URLDecoder.decode(it, Charsets.UTF_8) }
            val mainPartAndMetadata = decodedValue.split("+", limit = 2)
            val mainPart = mainPartAndMetadata[0]
            val metadata = mainPartAndMetadata.getOrNull(1) ?: ""

            val separator = SEPARATORS.find { it in mainPart }
            val beforeSeparator = if (separator == null) mainPart else mainPart.substringBefore(separator)
            val partCount = beforeSeparator.count { it == '.' } + 1
            val parts = mainPart.split('.', limit = partCount).map { part ->
                if (separator != null && separator in part) {
                    val subParts = part.split(separator, limit = 2)
                    parsePreReleasePart(subParts[0], subParts[1], separator, part)
                } else {
                    // Forge has a single version which should be 14.8.* but is actually 14.v8.*
                    val numberPart = if (part.startsWith('v')) {
                        part.substring(1)
                    } else {
                        part
                    }
                    ReleasePart(parseInt(numberPart), part)
                }
            }
            return SemanticVersion(parts, metadata)
        }

        sealed class VersionPart : Comparable<VersionPart> {
            abstract val versionString: String

            data class ReleasePart(val version: Int, override val versionString: String) : VersionPart() {
                override fun compareTo(other: VersionPart) =
                    when (other) {
                        is ReleasePart -> version - other.version
                        is TextPart -> 1
                        is PreReleasePart -> if (version != other.version) version - other.version else 1
                    }
            }

            data class TextPart(override val versionString: String) : VersionPart() {
                private val priority = TEXT_PRIORITIES[versionString] ?: -1

                override fun compareTo(other: VersionPart) =
                    when (other) {
                        is ReleasePart -> -1
                        is PreReleasePart -> -1
                        is TextPart ->
                            if (priority != other.priority) {
                                priority - other.priority
                            } else {
                                versionString.compareTo(other.versionString)
                            }
                    }

                override fun hashCode() = versionString.hashCode()

                override fun equals(other: Any?) =
                    when (other) {
                        is TextPart -> versionString == other.versionString
                        else -> false
                    }
            }

            data class PreReleasePart(
                val version: Int,
                val separator: Char,
                val subParts: List<VersionPart>,
                override val versionString: String,
            ) : VersionPart() {

                override fun compareTo(other: VersionPart): Int =
                    when (other) {
                        is ReleasePart -> if (version != other.version) version - other.version else -1
                        is TextPart -> 1
                        is PreReleasePart ->
                            if (version != other.version) {
                                version - other.version
                            } else {
                                naturalOrder<VersionPart>().lexicographical().compare(subParts, other.subParts)
                            }
                    }

                override fun hashCode(): Int {
                    return version + 31 * subParts.hashCode()
                }

                override fun equals(other: Any?): Boolean {
                    return when (other) {
                        is PreReleasePart -> other.version == version && other.subParts == subParts
                        else -> false
                    }
                }
            }
        }
    }
}
