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
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AtFile extends PsiFileBase {
    public AtFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, AtLanguage.getInstance());
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return AtFileType.getInstance();
    }

    @Override
    public String toString() {
        return "Access Transformer File";
    }

    @Nullable
    @Override
    public Icon getIcon(int flags) {
        return PlatformAssets.MCP_ICON;
    }
}
