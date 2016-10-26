/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory;
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtFunctionMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class AtFunctionImplMixin extends ASTWrapperPsiElement implements AtFunctionMixin {

    public AtFunctionImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setArgumentList(@NotNull String arguments) {
        final String funcName = getFuncName().getNameElement().getText();
        getArgumentList().forEach(PsiElement::delete);
        //noinspection ConstantConditions
        final String returnValue = getReturnValue().getClassValue().getText();
        replace(AtElementFactory.createFunction(getProject(), funcName + "(" + arguments + ")" + returnValue));
    }

    public void setReturnValue(@NotNull String returnValue) {
        getReturnValue().replace(AtElementFactory.createReturnValue(getProject(), returnValue));
    }

    public void setFunction(@NotNull String function) {
        replace(AtElementFactory.createFunction(getProject(), function));
    }
}
