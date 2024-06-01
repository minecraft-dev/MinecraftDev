package com.demonwav.mcdev.creator.custom.model

import com.demonwav.mcdev.util.SemanticVersion

@TemplateApi
data class ForgeVersions(
    val minecraft: SemanticVersion,
    val forge: SemanticVersion,
) : HasMinecraftVersion {
    override val minecraftVersion = minecraft

    val minecraftNext by lazy {
        val mcNext = when (val part = minecraft.parts.getOrNull(1)) {
            // Mimics the code used to get the next Minecraft version in Forge's MDK
            // https://github.com/MinecraftForge/MinecraftForge/blob/0ff8a596fc1ef33d4070be89dd5cb4851f93f731/build.gradle#L884
            is SemanticVersion.Companion.VersionPart.ReleasePart -> (part.version + 1).toString()
            null -> "?"
            else -> part.versionString
        }

        "1.$mcNext"
    }
    val forgeSpec by lazy { forge.parts[0].versionString }
}
