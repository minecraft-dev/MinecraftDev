package com.demonwav.mcdev.creator.custom.model

import com.demonwav.mcdev.util.SemanticVersion

@TemplateApi
interface HasMinecraftVersion {

    val minecraftVersion: SemanticVersion
}
