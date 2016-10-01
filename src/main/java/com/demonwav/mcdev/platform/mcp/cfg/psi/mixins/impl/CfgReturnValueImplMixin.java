package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.impl;

import static com.demonwav.mcdev.platform.mcp.cfg.psi.CfgMixinUtil.getClassFromString;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.CfgReturnValueMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CfgReturnValueImplMixin extends ASTWrapperPsiElement implements CfgReturnValueMixin {
    public CfgReturnValueImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public PsiClass getReturnValueClass() {
        return getClassFromString(getReturnValueText(), getProject());
    }

    @NotNull
    public String getReturnValueText() {
        //noinspection ConstantConditions
        return getPrimitive() != null ? getPrimitive().getText() : getClassValue() != null ? getClassValue().getText() : null;
    }

    public void setReturnValue(@NotNull String returnValue) {
        replace(CfgElementFactory.createReturnValue(getProject(), returnValue));
    }
}
