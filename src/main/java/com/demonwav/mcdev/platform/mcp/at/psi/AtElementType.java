/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi;

import com.demonwav.mcdev.platform.mcp.at.AtLanguage;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AtElementType extends IElementType {
    public AtElementType(@NotNull @NonNls String debugName) {
        super(debugName, AtLanguage.getInstance());
    }
}
