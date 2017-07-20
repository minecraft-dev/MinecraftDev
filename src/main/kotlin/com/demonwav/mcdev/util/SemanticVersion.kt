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

class SemanticVersion(val value: String) : Comparable<SemanticVersion> {
    val parts = value.split(".").map {
        if (it.contains("_pre")) {
            val subParts = it.split("_pre")
            PreReleasePart(subParts[0].toInt(), subParts[1].toInt())
        } else {
            ReleasePart(it.toInt())
        }
    }

    override fun compareTo(other: SemanticVersion): Int {
        val result = parts.zip(other.parts).fold(0, { acc, (a, b) -> if (acc == -1) acc else a.compareTo(b) })
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

    override fun hashCode(): Int {
        return parts.hashCode()
    }

    companion object {
        sealed class VersionPart : Comparable<VersionPart> {
            data class ReleasePart(val version: Int) : VersionPart() {
                override fun compareTo(other: VersionPart) =
                    when (other) {
                        is PreReleasePart -> if (version != other.version) version - other.version else 1
                        is ReleasePart -> version - other.version
                    }
            }

            data class PreReleasePart(val version: Int, val pre: Int) : VersionPart() {
                override fun compareTo(other: VersionPart) =
                    when (other) {
                        is PreReleasePart -> if (version != other.version) version - other.version else pre - other.pre
                        is ReleasePart -> if (version != other.version) version - other.version else -1
                    }
            }
        }
    }
}
