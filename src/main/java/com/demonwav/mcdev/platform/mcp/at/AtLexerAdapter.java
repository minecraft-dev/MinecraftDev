/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.mcp.at;

import com.intellij.lexer.FlexAdapter;

public class AtLexerAdapter extends FlexAdapter {
    public AtLexerAdapter() {
        super(new AtLexer());
    }
}
