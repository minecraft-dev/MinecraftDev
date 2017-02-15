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
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtClassNameMixin;
import com.demonwav.mcdev.util.McPsiClass;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AtClassNameImplMixin extends ASTWrapperPsiElement implements AtClassNameMixin {

    public AtClassNameImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public PsiClass getClassNameValue() {
        return McPsiClass.findQualifiedClass(getProject(), getClassNameText());
    }

    @NotNull
    public String getClassNameText() {
        return getClassNameElement().getText();
    }

    public void setClassName(@NotNull String className) {
        replace(AtElementFactory.createClassName(getProject(), className));
    }
}
