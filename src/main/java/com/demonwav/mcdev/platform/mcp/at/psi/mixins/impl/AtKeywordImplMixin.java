package com.demonwav.mcdev.platform.mcp.at.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory;
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtKeywordMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class AtKeywordImplMixin extends ASTWrapperPsiElement implements AtKeywordMixin {
    public AtKeywordImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public AtElementFactory.Keyword getKeywordValue() {
        //noinspection ConstantConditions
        return AtElementFactory.Keyword.match(getKeywordElement().getText());
    }

    public void setKeyword(@NotNull AtElementFactory.Keyword keyword) {
        replace(AtElementFactory.createKeyword(getProject(), keyword));
    }
}
