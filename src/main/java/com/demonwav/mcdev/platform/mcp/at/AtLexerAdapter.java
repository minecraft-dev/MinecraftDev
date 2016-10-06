package com.demonwav.mcdev.platform.mcp.at;

import com.intellij.lexer.FlexAdapter;

public class AtLexerAdapter extends FlexAdapter {
    public AtLexerAdapter() {
        super(new AtLexer());
    }
}
