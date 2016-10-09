/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.mcp.at.psi.mixins;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface AtFieldNameMixin extends PsiElement {

    @NotNull
    PsiElement getNameElement();

    void setFieldName(@NotNull String fieldName);

    @NotNull
    String getFieldNameText();
}
