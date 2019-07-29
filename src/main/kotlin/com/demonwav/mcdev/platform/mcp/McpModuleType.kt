/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.demonwav.mcdev.util.CommonColors
import javax.swing.Icon

object McpModuleType : AbstractModuleType<McpModule>("", "") {

    private const val ID = "MCP_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, McpConstants.TEXT_FORMATTING)
    }

    override val platformType = PlatformType.MCP
    override val icon: Icon? = null
    override val id = ID
    override val ignoredAnnotations = emptyList<String>()
    override val listenerAnnotations = emptyList<String>()
    override val hasIcon = false

    override fun generateModule(facet: MinecraftFacet) = McpModule(facet)
}
