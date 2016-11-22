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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AtArgumentMixin extends PsiElement {

    @Nullable
    PsiElement getClassValue();

    @Nullable
    PsiElement getPrimitive();

    @Nullable
    PsiClass getArgumentClass();

    @NotNull
    String getArgumentText();

    void setArgument(@NotNull String argument);
}
