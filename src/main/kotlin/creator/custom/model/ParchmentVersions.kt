package com.demonwav.mcdev.creator.custom.model

import com.demonwav.mcdev.util.SemanticVersion

data class ParchmentVersions(
    val use: Boolean,
    val version: SemanticVersion,
    override val minecraftVersion: SemanticVersion,
    val includeOlderMcVersions: Boolean,
    val includeSnapshots: Boolean,
) : HasMinecraftVersion
