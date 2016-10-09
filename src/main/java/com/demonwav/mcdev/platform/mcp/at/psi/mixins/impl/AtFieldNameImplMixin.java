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

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory;
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtFieldNameMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class AtFieldNameImplMixin extends ASTWrapperPsiElement implements AtFieldNameMixin {
    public AtFieldNameImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setFieldName(@NotNull String fieldName) {
        replace(AtElementFactory.createFieldName(getProject(), fieldName));
    }

    @NotNull
    public String getFieldNameText() {
        return getNameElement().getText();
    }
}
