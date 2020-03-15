/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
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

%s CLASS_NAME
%s MEMBER_NAME
%s TYPES

%unicode

PRIMITIVE=[ZBCSIFDJV]
CLASS_VALUE=(\[+[ZBCSIFDJ]|(\[*L[^;\n]+;))
KEYWORD_ELEMENT=(public|private|protected|default)([-+]f)?
NAME_ELEMENT=([\p{L}_\p{Sc}][\p{L}\p{N}_\p{Sc}]*)|<init>
CLASS_NAME_ELEMENT=([\p{L}_\p{Sc}][\p{L}\p{N}_\p{Sc}]*\.)*[\p{L}_\p{Sc}][\p{L}\p{N}_\p{Sc}]*
COMMENT=#.*
CRLF=\n|\r|\r\n
WHITE_SPACE=\s

%%

<YYINITIAL> {
    {KEYWORD_ELEMENT}                           { yybegin(CLASS_NAME); return KEYWORD_ELEMENT; }
}

<CLASS_NAME> {
    {CLASS_NAME_ELEMENT}                        { yybegin(MEMBER_NAME); return CLASS_NAME_ELEMENT; }
}

<MEMBER_NAME> {
    "*" | "*()"                                 { return ASTERISK_ELEMENT; }
    {NAME_ELEMENT}                              { yybegin(TYPES); return NAME_ELEMENT; }
}

<TYPES> {
    "("                                         { return OPEN_PAREN; }
    ")"                                         { return CLOSE_PAREN; }
    {CLASS_VALUE}                               { return CLASS_VALUE; }
    {PRIMITIVE} ({PRIMITIVE}|{CLASS_VALUE})*    { zzMarkedPos = zzStartRead + 1; return PRIMITIVE; }
}

{CRLF}                                          { yybegin(YYINITIAL); return CRLF; }
{WHITE_SPACE}                                   { return WHITE_SPACE; }

{COMMENT}                                       { return COMMENT; }
[^]                                             { return BAD_CHARACTER; }
