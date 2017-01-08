/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi.mixins;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AtClassNameMixin extends PsiElement {

    @NotNull
    PsiElement getClassNameElement();

    @Nullable
    PsiClass getClassNameValue();

    @NotNull
    String getClassNameText();

    void setClassName(@NotNull String className);
}
