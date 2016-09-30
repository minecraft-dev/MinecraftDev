package com.demonwav.mcdev.platform.forge.cfg;

import com.demonwav.mcdev.asset.PlatformAssets;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class CfgFile extends PsiFileBase {
    public CfgFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, CfgLanguage.getInstance());
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return CfgFileType.getInstance();
    }

    @Override
    public String toString() {
        return "Cfg file";
    }

    @Nullable
    @Override
    public Icon getIcon(int flags) {
        return PlatformAssets.MIXIN;
    }
}
