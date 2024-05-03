package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.util.SemanticVersion

typealias DerivationMethod = (from: Any?, properties: Map<String, Any?>, parameters: Map<String, Any?>) -> Any?

object DerivationMethods {

    val methods: Map<String, DerivationMethod> = mapOf(
        "extractVersionMajorMinor" to ::extractVersionMajorMinor
    )

    fun extractVersionMajorMinor(from: Any?, properties: Map<String, Any?>, parameters: Map<String, Any?>): SemanticVersion? {
        if (from !is String) {
            return null
        }

        val version = SemanticVersion.parse(from)
        if (version.parts.size < 2) {
            return null
        }

        val (part1, part2) = version.parts
        if (part1 is SemanticVersion.Companion.VersionPart.ReleasePart &&
            part2 is SemanticVersion.Companion.VersionPart.ReleasePart
        ) {
            return SemanticVersion(listOf(part1, part2))
        }

        return null
    }
}
