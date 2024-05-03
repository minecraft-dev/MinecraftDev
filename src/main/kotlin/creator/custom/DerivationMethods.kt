package com.demonwav.mcdev.creator.custom

import com.demonwav.mcdev.util.SemanticVersion

object DerivationMethods {

    val methods: Map<String, (Any?) -> Any?> = mapOf(
        "extractVersionMajorMinor" to { extractVersionMajorMinor(it as? String) }
    )

    fun extractVersionMajorMinor(versionString: String?): SemanticVersion? {
        if (versionString == null) {
            return null
        }

        val version = SemanticVersion.parse(versionString)
        if (version.parts.size < 2) {
            return null
        }

        val (part1, part2) = version.parts
        if (part1 is SemanticVersion.Companion.VersionPart.ReleasePart &&
            part2 is SemanticVersion.Companion.VersionPart.ReleasePart) {
            return SemanticVersion(listOf(part1, part2))
        }

        return null
    }
}
