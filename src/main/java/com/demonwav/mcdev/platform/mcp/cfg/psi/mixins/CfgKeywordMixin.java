package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface CfgKeywordMixin extends PsiElement {

    @NotNull
    PsiElement getKeywordElement();

    @NotNull
    CfgElementFactory.Keyword getKeywordValue();

    void setKeyword(@NotNull CfgElementFactory.Keyword keyword);
}
