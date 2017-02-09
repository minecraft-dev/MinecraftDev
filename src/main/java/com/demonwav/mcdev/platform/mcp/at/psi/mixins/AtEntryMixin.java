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

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtAsterisk;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtClassName;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtKeyword;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AtEntryMixin extends PsiElement {

    @Nullable
    AtAsterisk getAsterisk();

    @NotNull
    AtClassName getClassName();

    @Nullable
    AtFieldName getFieldName();

    @Nullable
    AtFunction getFunction();

    @NotNull
    AtKeyword getKeyword();

    void setEntry(@NotNull String entry);

    void setKeyword(@NotNull AtElementFactory.Keyword keyword);

    void setClassName(@NotNull String className);

    void setFieldName(@NotNull String fieldName);

    void setFunction(@NotNull String function);

    void setAsterisk();
}
