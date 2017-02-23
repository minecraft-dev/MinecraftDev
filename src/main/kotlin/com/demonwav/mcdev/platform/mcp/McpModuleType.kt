/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp

import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.openapi.module.Module

object McpModuleType : AbstractModuleType<McpModule>("", "") {

    private const val ID = "MCP_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, McpConstants.TEXT_FORMATTING)
    }

    override fun getPlatformType() = PlatformType.MCP
    override fun getIcon() = null
    override fun getId() = ID
    override fun getIgnoredAnnotations() = emptyList<String>()
    override fun getListenerAnnotations() = emptyList<String>()
    override fun generateModule(module: Module) = McpModule(module)
    override fun hasIcon() = false
}
