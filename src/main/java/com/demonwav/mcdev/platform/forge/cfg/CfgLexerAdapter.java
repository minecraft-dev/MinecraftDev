package com.demonwav.mcdev.platform.forge.cfg;

import com.intellij.lexer.FlexAdapter;

public class CfgLexerAdapter extends FlexAdapter {
    public CfgLexerAdapter() {
        super(new CfgLexer());
    }
}
