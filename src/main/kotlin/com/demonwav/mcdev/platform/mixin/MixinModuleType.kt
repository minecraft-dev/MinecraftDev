/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType

object MixinModuleType : AbstractModuleType<MixinModule>("org.spongepowered", "mixin") {

    const val ID = "MIXIN_MODULE_TYPE"

    override fun getPlatformType() = PlatformType.MIXIN
    override fun getIcon() = null
    override fun hasIcon() = false

    override fun getId(): String = ID

    override fun getIgnoredAnnotations() = emptyList<String>()
    override fun getListenerAnnotations() = emptyList<String>()

    override fun generateModule(facet: MinecraftFacet) = MixinModule(facet)
}
