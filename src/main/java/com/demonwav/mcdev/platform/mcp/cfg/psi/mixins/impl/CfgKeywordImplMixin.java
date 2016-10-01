package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.CfgKeywordMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class CfgKeywordImplMixin extends ASTWrapperPsiElement implements CfgKeywordMixin {
    public CfgKeywordImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public CfgElementFactory.Keyword getKeywordValue() {
        //noinspection ConstantConditions
        return CfgElementFactory.Keyword.match(getKeywordElement().getText());
    }

    public void setKeyword(@NotNull CfgElementFactory.Keyword keyword) {
        replace(CfgElementFactory.createKeyword(getProject(), keyword));
    }
}
