package com.demonwav.mcdev.creator.custom.model

import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.util.SemanticVersion

@TemplateApi
data class FabricVersionsModel(
    override val minecraftVersion: SemanticVersion,
    val loom: SemanticVersion,
    val loader: SemanticVersion,
    val yarn: FabricVersions.YarnVersion,
    val useFabricApi: Boolean,
    val fabricApi: SemanticVersion,
    val useOfficialMappings: Boolean,
) : HasMinecraftVersion
