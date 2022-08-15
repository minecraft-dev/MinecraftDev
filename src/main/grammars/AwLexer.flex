/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw.gen;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes.*;
import static com.intellij.psi.TokenType.*;

%%

%{
    public AwLexer() {
        this((java.io.Reader)null);
    }
%}

%public
%class AwLexer
%implements FlexLexer
%function advance
%type IElementType

%s HEADER
%s CLASS_NAME
%s MEMBER_NAME
%s TYPES

%unicode

HEADER_NAME=accessWidener
HEADER_VERSION_ELEMENT=v\d+
HEADER_NAMESPACE_ELEMENT=named|intermediary
PRIMITIVE=[ZBCSIFDJV]
CLASS_VALUE=(\[+[ZBCSIFDJ]|(\[*L[^;\n]+;))
ACCESS_ELEMENT=accessible|transitive-accessible|extendable|transitive-extendable|mutable|transitive-mutable
CLASS_ELEMENT=class
METHOD_ELEMENT=method
FIELD_ELEMENT=field
NAME_ELEMENT=\w+|<init>
CLASS_NAME_ELEMENT=(\w+\/)*\w+(\$\w+)*
COMMENT=#.*
CRLF=\n|\r|\r\n
WHITE_SPACE=\s

%%

<YYINITIAL> {
    {HEADER_NAME}                               { yybegin(HEADER); return HEADER_NAME; }
    {ACCESS_ELEMENT}                            { return ACCESS_ELEMENT; }
    {CLASS_ELEMENT}                             { yybegin(CLASS_NAME); return CLASS_ELEMENT; }
    {METHOD_ELEMENT}                            { yybegin(CLASS_NAME); return METHOD_ELEMENT; }
    {FIELD_ELEMENT}                             { yybegin(CLASS_NAME); return FIELD_ELEMENT; }
}

<HEADER> {
    {HEADER_VERSION_ELEMENT}                    { return HEADER_VERSION_ELEMENT; }
    {HEADER_NAMESPACE_ELEMENT}                  { return HEADER_NAMESPACE_ELEMENT; }
}

<CLASS_NAME> {
    {CLASS_NAME_ELEMENT}                        { yybegin(MEMBER_NAME); return CLASS_NAME_ELEMENT; }
}

<MEMBER_NAME> {
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
