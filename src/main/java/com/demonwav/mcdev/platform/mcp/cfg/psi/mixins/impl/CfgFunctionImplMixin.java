package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.CfgFunctionMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class CfgFunctionImplMixin extends ASTWrapperPsiElement implements CfgFunctionMixin {

    public CfgFunctionImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setArgumentList(@NotNull String arguments) {
        final String funcName = getFuncName().getNameElement().getText();
        getArgumentList().forEach(PsiElement::delete);
        //noinspection ConstantConditions
        final String returnValue = getReturnValue().getClassValue().getText();
        replace(CfgElementFactory.createFunction(getProject(), funcName + "(" + arguments + ")" + returnValue));
    }

    public void setReturnValue(@NotNull String returnValue) {
        getReturnValue().replace(CfgElementFactory.createReturnValue(getProject(), returnValue));
    }

    public void setFunction(@NotNull String function) {
        replace(CfgElementFactory.createFunction(getProject(), function));
    }
}
