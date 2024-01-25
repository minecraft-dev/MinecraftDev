/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.expression;

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

%%

%public
%class MEExpressionLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

WHITE_SPACE=[\ \n\t\r]
RESERVED=assert|break|case|catch|const|continue|default|do|else|finally|for|goto|if|switch|synchronized|try|while|yield|_
WILDCARD="?"
NEW=new
INSTANCEOF=instanceof
BOOL_LIT=true|false
NULL_LIT=null
RETURN=return
THROW=throw
THIS=this
SUPER=super
CLASS=class
IDENTIFIER=[A-Za-z_][A-Za-z0-9_]*
INT_LIT=([0-9]+|0x[0-9a-fA-F]+)
DEC_LIT=[0-9]*\.[0-9]+
PLUS="+"
MINUS=-
MULT="*"
DIV="/"
MOD=%
BITWISE_NOT="~"
DOT="."
COMMA=,
LEFT_PAREN="("
RIGHT_PAREN=")"
LEFT_BRACKET="["
RIGHT_BRACKET="]"
LEFT_BRACE="{"
RIGHT_BRACE="}"
AT=@
SHL=<<
SHR=>>
USHR=>>>
LT=<
LE=<=
GT=>
GE=>=
EQ===
NE="!="
BITWISE_AND=&
BITWISE_XOR="^"
BITWISE_OR="|"
ASSIGN==

STRING_TERMINATOR='
STRING_ESCAPE=\\'|\\\\

%state STRING

%%

<YYINITIAL> {WHITE_SPACE}+ { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
<YYINITIAL> {RESERVED} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_RESERVED; }
<YYINITIAL> {WILDCARD} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_WILDCARD; }
<YYINITIAL> {NEW} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_NEW; }
<YYINITIAL> {INSTANCEOF} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_INSTANCEOF; }
<YYINITIAL> {BOOL_LIT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_BOOL_LIT; }
<YYINITIAL> {NULL_LIT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_NULL_LIT; }
<YYINITIAL> {RETURN} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_RETURN; }
<YYINITIAL> {THROW} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_THROW; }
<YYINITIAL> {THIS} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_THIS; }
<YYINITIAL> {SUPER} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_SUPER; }
<YYINITIAL> {CLASS} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_CLASS; }
<YYINITIAL> {IDENTIFIER} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_IDENTIFIER; }
<YYINITIAL> {INT_LIT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_INT_LIT; }
<YYINITIAL> {DEC_LIT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_DEC_LIT; }
<YYINITIAL> {PLUS} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_PLUS; }
<YYINITIAL> {MINUS} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_MINUS; }
<YYINITIAL> {MULT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_MULT; }
<YYINITIAL> {DIV} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_DIV; }
<YYINITIAL> {MOD} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_MOD; }
<YYINITIAL> {BITWISE_NOT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_BITWISE_NOT; }
<YYINITIAL> {DOT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_DOT; }
<YYINITIAL> {COMMA} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_COMMA; }
<YYINITIAL> {LEFT_PAREN} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_LEFT_PAREN; }
<YYINITIAL> {RIGHT_PAREN} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_RIGHT_PAREN; }
<YYINITIAL> {LEFT_BRACKET} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_LEFT_BRACKET; }
<YYINITIAL> {RIGHT_BRACKET} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_RIGHT_BRACKET; }
<YYINITIAL> {LEFT_BRACE} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_LEFT_BRACE; }
<YYINITIAL> {RIGHT_BRACE} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_RIGHT_BRACE; }
<YYINITIAL> {AT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_AT; }
<YYINITIAL> {SHL} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_SHL; }
<YYINITIAL> {SHR} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_SHR; }
<YYINITIAL> {USHR} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_USHR; }
<YYINITIAL> {LT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_LT; }
<YYINITIAL> {LE} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_LE; }
<YYINITIAL> {GT} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_GT; }
<YYINITIAL> {GE} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_GE; }
<YYINITIAL> {EQ} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_EQ; }
<YYINITIAL> {NE} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_NE; }
<YYINITIAL> {BITWISE_AND} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_BITWISE_AND; }
<YYINITIAL> {BITWISE_XOR} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_BITWISE_XOR; }
<YYINITIAL> {BITWISE_OR} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_BITWISE_OR; }
<YYINITIAL> {ASSIGN} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_ASSIGN; }

<YYINITIAL> {STRING_TERMINATOR} { yybegin(STRING); return MEExpressionTypes.TOKEN_STRING_TERMINATOR; }
<STRING> {STRING_ESCAPE} { yybegin(STRING); return MEExpressionTypes.TOKEN_STRING_ESCAPE; }
<STRING> {STRING_TERMINATOR} { yybegin(YYINITIAL); return MEExpressionTypes.TOKEN_STRING_TERMINATOR; }
<STRING> [^'\\]+ { yybegin(STRING); return MEExpressionTypes.TOKEN_STRING; }

[^] { return TokenType.BAD_CHARACTER; }
