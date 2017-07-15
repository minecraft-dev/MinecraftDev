/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang.gen;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes.*;
import static com.intellij.psi.TokenType.*;

%%

%{
    public I18nLexer() {
        this((java.io.Reader)null);
    }
%}

%public
%class I18nLexer
%implements FlexLexer
%function advance
%type IElementType

%s WAITING_VALUE
%s WAITING_EQUALS

%unicode

EOL_WS              = \n | \r | \r\n
WHITE_SPACE_CHAR    = {EOL_WS}
WHITE_SPACE         = {WHITE_SPACE_CHAR}+

KEY = [^=#\n\r][^=\n\r]*
VALUE = [^\n\r]+
COMMENT = #[^\n\r]+

%%

<YYINITIAL> {
    {KEY}                       { yybegin(WAITING_EQUALS); return KEY; }
    "="                         { yybegin(WAITING_VALUE); return EQUALS; }
    {COMMENT}                   { return COMMENT; }
    {WHITE_SPACE}               { return WHITE_SPACE; }
}

<WAITING_EQUALS> {
    "="                         { yybegin(WAITING_VALUE); return EQUALS; }
}

<WAITING_VALUE> {
    {WHITE_SPACE}               { yybegin(YYINITIAL); return WHITE_SPACE; }
    {VALUE}                     { yybegin(YYINITIAL); return VALUE; }
}

[^]                             { return BAD_CHARACTER; }
