package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.CfgFieldNameMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class CfgFieldNameImplMixin extends ASTWrapperPsiElement implements CfgFieldNameMixin {
    public CfgFieldNameImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setFieldName(@NotNull String fieldName) {
        replace(CfgElementFactory.createFieldName(getProject(), fieldName));
    }

    @NotNull
    public String getFieldNameText() {
        return getNameElement().getText();
    }
}
