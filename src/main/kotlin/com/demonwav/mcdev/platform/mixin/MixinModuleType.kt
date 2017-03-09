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
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations

object MixinModuleType : AbstractModuleType<MixinModule>("org.spongepowered", "mixin") {

    const val ID = "MIXIN_MODULE_TYPE"

    private val IGNORED_ANNOTATIONS = listOf(
        Annotations.DEBUG,
        Annotations.FINAL,
        Annotations.IMPLEMENTS,
        Annotations.INTERFACE,
        Annotations.INTRINSIC,
        Annotations.MIXIN,
        Annotations.MUTABLE,
        Annotations.OVERWRITE,
        Annotations.SHADOW,
        Annotations.SOFT_OVERRIDE,
        Annotations.UNIQUE,
        Annotations.INJECT,
        Annotations.MODIFY_ARG,
        Annotations.MODIFY_CONSTANT,
        Annotations.MODIFY_VARIABLE,
        Annotations.REDIRECT,
        Annotations.SURROGATE
    )

    private val LISTENER_ANNOTATIONS = emptyList<String>()

    override fun getPlatformType() = PlatformType.MIXIN
    override fun getIcon() = null
    override fun hasIcon() = false

    override fun getId(): String = ID

    override fun getIgnoredAnnotations() = IGNORED_ANNOTATIONS
    override fun getListenerAnnotations() = LISTENER_ANNOTATIONS

    override fun generateModule(facet: MinecraftFacet) = MixinModule(facet)
}
