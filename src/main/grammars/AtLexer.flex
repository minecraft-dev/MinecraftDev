/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.gen;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes.*;
import static com.intellij.psi.TokenType.*;

%%

%{
    public AtLexer() {
        this((java.io.Reader)null);
    }
%}

%public
%class AtLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

PRIMITIVE=[ZBCSIFDJV]
CLASS_VALUE=\[*([ZBCSIFDJ]|L[^;\n]+;)
KEYWORD_ELEMENT=(public|private|protected|default)([-+]f)?
NAME_ELEMENT=[a-zA-Z0-9_]+|<init>
CLASS_NAME_ELEMENT=[a-zA-Z_$0-9\.]*[a-zA-Z_$0-9]
COMMENT=#.*
CRLF=\n|\r|\r\n
WHITE_SPACE=\s

%%

<YYINITIAL> {
    {CRLF}                                      { return CRLF; }
    {WHITE_SPACE}                               { return WHITE_SPACE; }

    "("                                         { return OPEN_PAREN; }
    ")"                                         { return CLOSE_PAREN; }
    "*" | "*()"                                 { return ASTERISK_ELEMENT; }

    {PRIMITIVE} ({PRIMITIVE}|{CLASS_VALUE})*    { zzMarkedPos = zzStartRead + 1; return PRIMITIVE; }

    {CLASS_VALUE}                               { return CLASS_VALUE; }
    {KEYWORD_ELEMENT}                           { return KEYWORD_ELEMENT; }
    {NAME_ELEMENT}                              { return NAME_ELEMENT; }
    {CLASS_NAME_ELEMENT}                        { return CLASS_NAME_ELEMENT; }
    {COMMENT}                                   { return COMMENT; }
}

[^]                                             { return BAD_CHARACTER; }
