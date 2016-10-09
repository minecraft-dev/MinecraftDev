/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.mcp.at.psi.mixins.impl;

import static com.demonwav.mcdev.platform.mcp.at.psi.AtMixinUtil.getClassFromString;

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory;
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtReturnValueMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AtReturnValueImplMixin extends ASTWrapperPsiElement implements AtReturnValueMixin {
    public AtReturnValueImplMixin(@NotNull ASTNode node) {
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
        replace(AtElementFactory.createReturnValue(getProject(), returnValue));
    }
}
