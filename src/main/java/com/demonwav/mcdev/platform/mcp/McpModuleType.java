/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.mcp.util.McpConstants;
import com.demonwav.mcdev.util.CommonColors;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

public class McpModuleType extends AbstractModuleType<McpModule> {

    private static final McpModuleType instance = new McpModuleType();

    public static final String ID = "MCP_MODULE_TYPE";

    public static McpModuleType getInstance() {
        return instance;
    }

    public McpModuleType() {
        super("", "");
        CommonColors.applyStandardColors(this.colorMap, McpConstants.TEXT_FORMATTING);
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.MCP;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean hasIcon() {
        return false;
    }

    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public McpModule generateModule(Module module) {
        return new McpModule(module);
    }
}
