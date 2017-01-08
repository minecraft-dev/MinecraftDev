/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory;
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtFuncNameMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class AtFuncNameImplMixin extends ASTWrapperPsiElement implements AtFuncNameMixin {
    public AtFuncNameImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setFuncName(@NotNull String funcName) {
        replace(AtElementFactory.createFuncName(getProject(), funcName));
    }

    @NotNull
    public String getFuncNameText() {
        return getNameElement().getText();
    }
}
