/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.lang.gen;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.demonwav.mcdev.translations.lang.gen.psi.LangTypes.*;
import static com.intellij.psi.TokenType.*;

%%

%{
    public LangLexer() {
        this((java.io.Reader)null);
    }
%}

%public
%class LangLexer
%implements FlexLexer
%function advance
%type IElementType

%s WAITING_VALUE
%s WAITING_EQUALS

%unicode

EOL_WS              = \n | \r | \r\n
LINE_ENDING         = {EOL_WS}

KEY = [^=#\n\r][^=\n\r]*
VALUE = [^\n\r]+
COMMENT = #[^\n\r]*

%%

<YYINITIAL> {
    {KEY}/"="                   { yybegin(WAITING_EQUALS); return KEY; }
    {KEY}                       { return DUMMY; }
    {COMMENT}                   { return COMMENT; }
    {LINE_ENDING}               { return LINE_ENDING; }
}

<WAITING_EQUALS> {
    "="                         { yybegin(WAITING_VALUE); return EQUALS; }
}

<WAITING_VALUE> {
    {LINE_ENDING}               { yybegin(YYINITIAL); return LINE_ENDING; }
    {VALUE}                     { yybegin(YYINITIAL); return VALUE; }
}

[^]                             { return BAD_CHARACTER; }
