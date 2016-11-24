/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at;

import com.demonwav.mcdev.platform.mcp.at.gen.AtLexer;

import com.intellij.lexer.FlexAdapter;

public class AtLexerAdapter extends FlexAdapter {
    public AtLexerAdapter() {
        super(new AtLexer());
    }
}
