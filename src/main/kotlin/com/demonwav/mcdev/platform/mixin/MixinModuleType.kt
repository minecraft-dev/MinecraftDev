/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import javax.swing.Icon

object MixinModuleType : AbstractModuleType<MixinModule>("org.spongepowered", "mixin") {

    const val ID = "MIXIN_MODULE_TYPE"

    override val platformType = PlatformType.MIXIN
    override val icon: Icon? = null
    override val id = ID
    override val ignoredAnnotations = emptyList<String>()
    override val listenerAnnotations = emptyList<String>()
    override val hasIcon = false

    override fun generateModule(facet: MinecraftFacet) = MixinModule(facet)
}
