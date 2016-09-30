package com.demonwav.mcdev.platform.forge.cfg;

import com.demonwav.mcdev.asset.PlatformAssets;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class CfgFileType  extends LanguageFileType {
    private static final CfgFileType instance = new CfgFileType();

    public static CfgFileType getInstance() {
        return instance;
    }

    private CfgFileType() {
        super(CfgLanguage.getInstance());
    }

    @NotNull
    @Override
    public String getName() {
        return "cfg file";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "cfg mappings file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "cfg";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return PlatformAssets.MIXIN;
    }
}
