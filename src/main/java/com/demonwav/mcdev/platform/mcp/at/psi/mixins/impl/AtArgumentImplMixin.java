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
import com.demonwav.mcdev.platform.mcp.at.psi.AtMixinUtil;
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtArgumentMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AtArgumentImplMixin extends ASTWrapperPsiElement implements AtArgumentMixin {

    public AtArgumentImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public PsiClass getArgumentClass() {
        return AtMixinUtil.getClassFromString(getArgumentText(), getProject());
    }

    @NotNull
    public String getArgumentText() {
        //noinspection ConstantConditions
        return getClassValue() != null ? getClassValue().getText() : getPrimitive() != null ? getPrimitive().getText() : null;
    }

    public void setArgument(@NotNull String argument) {
        replace(AtElementFactory.createArgument(getProject(), argument));
    }
}
