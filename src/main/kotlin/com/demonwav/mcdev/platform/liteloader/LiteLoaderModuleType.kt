/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.module.Module

object LiteLoaderModuleType : AbstractModuleType<LiteLoaderModule>("", "") {

    private const val ID = "LITELOADER_MODULE_TYPE"

    val IGNORED_ANNOTATIONS = emptyList<String>()
    val LISTENER_ANNOTATIONS = emptyList<String>()

    override fun getPlatformType() = PlatformType.LITELOADER
    override fun getIcon() = PlatformAssets.LITELOADER_ICON
    override fun getId() = ID
    override fun getIgnoredAnnotations() = IGNORED_ANNOTATIONS
    override fun getListenerAnnotations() = LISTENER_ANNOTATIONS
    override fun generateModule(module: Module) = LiteLoaderModule(module)
}
