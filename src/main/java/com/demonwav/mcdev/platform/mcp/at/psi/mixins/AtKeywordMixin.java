/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi.mixins;

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface AtKeywordMixin extends PsiElement {

    @NotNull
    PsiElement getKeywordElement();

    @NotNull
    AtElementFactory.Keyword getKeywordValue();

    void setKeyword(@NotNull AtElementFactory.Keyword keyword);
}
