package com.demonwav.mcdev.platform.mcp.at.psi.mixins;

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface AtKeywordMixin extends PsiElement {

    @NotNull
    PsiElement getKeywordElement();

    @NotNull
    AtElementFactory.Keyword getKeywordValue();

    void setKeyword(@NotNull AtElementFactory.Keyword keyword);
}
