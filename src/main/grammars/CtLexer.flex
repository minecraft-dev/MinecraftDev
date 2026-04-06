/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mcp.ct.gen;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtTypes.*;
import static com.intellij.psi.TokenType.*;

%%

%{
    public CtLexer() {
        this((java.io.Reader)null);
    }
%}

%public
%class CtLexer
%implements FlexLexer
%function advance
%type IElementType

%s HEADER
%s AW_ENTRY
%s AW_CLASS_NAME
%s AW_MEMBER_NAME
%s AW_TYPES
%s ITF_ENTRY_KEY
%s ITF_ENTRY_VALUE
%s ENUM_NAME
%s ENUM_VALUE
%s SIGNATURE

%unicode

HEADER_NAME=accessWidener|classTweaker
HEADER_VERSION_ELEMENT=v\d+
HEADER_NAMESPACE_ELEMENT=\w+
PRIMITIVE=[ZBCSIFDJV]
CLASS_VALUE=L[^;\n]+;
SIGNATURE_CLASS_VALUE_START=L[^;<\n]+
TYPE_VARIABLE=T[^;\n]+;
ACCESS_ELEMENT=accessible|transitive-accessible|extendable|transitive-extendable|mutable|transitive-mutable
INJECT_INTERFACE_ELEMENT=inject-interface|transitive-inject-interface
EXTEND_ENUM_ELEMENT=extend-enum|transitive-extend-enum
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
    {ACCESS_ELEMENT}                            { yybegin(AW_ENTRY); return ACCESS_ELEMENT; }
    {INJECT_INTERFACE_ELEMENT}                  { yybegin(ITF_ENTRY_KEY); return INJECT_INTERFACE_ELEMENT; }
    {EXTEND_ENUM_ELEMENT}                       { yybegin(ENUM_NAME); return EXTEND_ENUM_ELEMENT; }
}

<HEADER> {
    {HEADER_VERSION_ELEMENT}                    { return HEADER_VERSION_ELEMENT; }
    {HEADER_NAMESPACE_ELEMENT}                  { return HEADER_NAMESPACE_ELEMENT; }
}

<AW_ENTRY> {
    {CLASS_ELEMENT}                             { yybegin(AW_CLASS_NAME); return CLASS_ELEMENT; }
    {METHOD_ELEMENT}                            { yybegin(AW_CLASS_NAME); return METHOD_ELEMENT; }
    {FIELD_ELEMENT}                             { yybegin(AW_CLASS_NAME); return FIELD_ELEMENT; }
}

<AW_CLASS_NAME> {
    {CLASS_NAME_ELEMENT}                        { yybegin(AW_MEMBER_NAME); return CLASS_NAME_ELEMENT; }
}

<AW_MEMBER_NAME> {
    {NAME_ELEMENT}                              { yybegin(AW_TYPES); return NAME_ELEMENT; }
}

<AW_TYPES> {
    "("                                         { return OPEN_PAREN; }
    ")"                                         { return CLOSE_PAREN; }
    "["                                         { return OPEN_BRACKET; }
    {CLASS_VALUE}                               { return CLASS_VALUE; }
    {PRIMITIVE}                                 { return PRIMITIVE; }
}

<ITF_ENTRY_KEY> {
    {CLASS_NAME_ELEMENT}                        { yybegin(ITF_ENTRY_VALUE); return CLASS_NAME_ELEMENT; }
}

<ITF_ENTRY_VALUE> {
    {CLASS_NAME_ELEMENT}                        { yybegin(SIGNATURE); return CLASS_NAME_ELEMENT; }
}

<ENUM_NAME> {
    {CLASS_NAME_ELEMENT}                        { yybegin(ENUM_VALUE); return CLASS_NAME_ELEMENT; }
}

<ENUM_VALUE> {
    {NAME_ELEMENT}                              { return NAME_ELEMENT; }
}

<SIGNATURE> {
    "<"                                         { return LESS_THAN; }
    ">"                                         { return GREATER_THAN; }
    "["                                         { return OPEN_BRACKET; }
    "+"                                         { return PLUS; }
    "-"                                         { return MINUS; }
    "*"                                         { return ASTERISK; }
    {SIGNATURE_CLASS_VALUE_START}               { return SIGNATURE_CLASS_VALUE_START; }
    ";"                                         { return SIGNATURE_CLASS_VALUE_END; }
    {PRIMITIVE}                                 { return PRIMITIVE; }
    {TYPE_VARIABLE}                             { return TYPE_VARIABLE; }
}

{CRLF}                                          { yybegin(YYINITIAL); return CRLF; }
{WHITE_SPACE}                                   { return WHITE_SPACE; }

{COMMENT}                                       { return COMMENT; }
[^]                                             { return BAD_CHARACTER; }
