/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at;

import com.demonwav.mcdev.asset.PlatformAssets;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class AtFileType extends LanguageFileType {
    private static final AtFileType instance = new AtFileType();

    public static AtFileType getInstance() {
        return instance;
    }

    private AtFileType() {
        super(AtLanguage.getInstance());
    }

    @NotNull
    @Override
    public String getName() {
        return "Access Transformers File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Access Transformers";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return PlatformAssets.MCP_ICON;
    }
}
