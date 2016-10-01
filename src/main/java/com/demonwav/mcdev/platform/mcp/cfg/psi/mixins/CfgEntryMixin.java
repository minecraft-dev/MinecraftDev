package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgAsterisk;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgClassName;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgFieldName;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgFunction;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgKeyword;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CfgEntryMixin extends PsiElement {

    @Nullable
    CfgAsterisk getAsterisk();

    @NotNull
    CfgClassName getClassName();

    @Nullable
    CfgFieldName getFieldName();

    @Nullable
    CfgFunction getFunction();

    @NotNull
    CfgKeyword getKeyword();

    void setEntry(@NotNull String entry);

    void setKeyword(@NotNull CfgElementFactory.Keyword keyword);

    void setClassName(@NotNull String className);

    void setFieldName(@NotNull String fieldName);

    void setFunction(@NotNull String function);

    void setAsterisk();
}
