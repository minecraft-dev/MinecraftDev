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

import com.demonwav.mcdev.platform.mcp.at.psi.AtArgument;
import com.demonwav.mcdev.platform.mcp.at.psi.AtFuncName;
import com.demonwav.mcdev.platform.mcp.at.psi.AtReturnValue;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AtFunctionMixin extends PsiElement {

    @NotNull
    List<AtArgument> getArgumentList();

    @NotNull
    AtFuncName getFuncName();

    @NotNull
    AtReturnValue getReturnValue();

    void setArgumentList(@NotNull String argumentList);

    void setReturnValue(@NotNull String returnValue);

    void setFunction(@NotNull String function);
}
