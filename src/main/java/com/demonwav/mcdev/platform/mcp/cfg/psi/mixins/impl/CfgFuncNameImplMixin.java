package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.CfgFuncNameMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class CfgFuncNameImplMixin extends ASTWrapperPsiElement implements CfgFuncNameMixin {
    public CfgFuncNameImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setFuncName(@NotNull String funcName) {
        replace(CfgElementFactory.createFuncName(getProject(), funcName));
    }

    @NotNull
    public String getFuncNameText() {
        return getNameElement().getText();
    }
}
