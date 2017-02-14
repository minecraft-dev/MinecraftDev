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

import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations

import com.google.common.collect.ImmutableList
import com.intellij.openapi.module.Module

internal object MixinModuleType : AbstractModuleType<MixinModule>("org.spongepowered", "mixin") {

    const val ID = "MIXIN_MODULE_TYPE"

    private val IGNORED_ANNOTATIONS = ImmutableList.builder<String>()
            .add(Annotations.DEBUG)
            .add(Annotations.FINAL)
            .add(Annotations.IMPLEMENTS)
            .add(Annotations.INTERFACE)
            .add(Annotations.INTRINSIC)
            .add(Annotations.MIXIN)
            .add(Annotations.MUTABLE)
            .add(Annotations.OVERWRITE)
            .add(Annotations.SHADOW)
            .add(Annotations.SOFT_OVERRIDE)
            .add(Annotations.UNIQUE)
            .add(Annotations.INJECT)
            .add(Annotations.MODIFY_ARG)
            .add(Annotations.MODIFY_CONSTANT)
            .add(Annotations.MODIFY_VARIABLE)
            .add(Annotations.REDIRECT)
            .add(Annotations.SURROGATE)
            .build()

    private val LISTENER_ANNOTATIONS = emptyList<String>()

    override fun getPlatformType() = PlatformType.MIXIN
    override fun getIcon() = null
    override fun hasIcon() = false

    override fun getId(): String = ID

    override fun getIgnoredAnnotations() = IGNORED_ANNOTATIONS
    override fun getListenerAnnotations() = LISTENER_ANNOTATIONS

    override fun generateModule(module: Module) = MixinModule(module)

}
