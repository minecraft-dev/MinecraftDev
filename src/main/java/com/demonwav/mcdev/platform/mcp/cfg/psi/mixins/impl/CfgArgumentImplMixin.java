package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgMixinUtil;
import com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.CfgArgumentMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CfgArgumentImplMixin extends ASTWrapperPsiElement implements CfgArgumentMixin {

    public CfgArgumentImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public PsiClass getArgumentClass() {
        return CfgMixinUtil.getClassFromString(getArgumentText(), getProject());
    }

    @NotNull
    public String getArgumentText() {
        //noinspection ConstantConditions
        return getClassValue() != null ? getClassValue().getText() : getPrimitive() != null ? getPrimitive().getText() : null;
    }

    public void setArgument(@NotNull String argument) {
        replace(CfgElementFactory.createArgument(getProject(), argument));
    }
}
