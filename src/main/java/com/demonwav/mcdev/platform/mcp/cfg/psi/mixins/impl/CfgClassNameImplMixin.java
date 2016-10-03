package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.CfgClassNameMixin;
import com.demonwav.mcdev.platform.mcp.util.McpUtil;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CfgClassNameImplMixin extends ASTWrapperPsiElement implements CfgClassNameMixin {

    public CfgClassNameImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public PsiClass getClassNameValue() {
        return McpUtil.getClassFromString(getClassNameText(), getProject());
    }

    @NotNull
    public String getClassNameText() {
        return getClassNameElement().getText();
    }

    public void setClassName(@NotNull String className) {
        replace(CfgElementFactory.createClassName(getProject(), className));
    }
}
