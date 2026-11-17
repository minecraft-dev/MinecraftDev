/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick.gen;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes.*;
import static com.intellij.psi.TokenType.*;

%%

%{
    // Whether the member signature currently being lexed is followed by a method descriptor (as opposed to a field
    // descriptor)
    private boolean methodDescriptor;
    // Whether the version in the header is one we know how to lex. A missing version is lexed as if it was supported.
    private boolean supportedVersion;
    private boolean seenVersion;

    public UnpickLexer() {
        this((java.io.Reader)null);
    }

    /**
     * Returns the length of the valid field descriptor at the start of the given text, or 0 if there isn't one.
     */
    private static int fieldDescriptorLength(CharSequence text, int start) {
        int pos = start;
        while (pos < text.length() && text.charAt(pos) == '[') {
            pos++;
        }
        if (pos == text.length()) {
            return 0;
        }
        switch (text.charAt(pos)) {
            case 'B': case 'C': case 'D': case 'F': case 'I': case 'J': case 'S': case 'Z':
                return pos + 1 - start;
            case 'L':
                pos++;
                int nameStart = pos;
                while (pos < text.length()) {
                    char c = text.charAt(pos);
                    if (c == ';') {
                        return pos == nameStart ? 0 : pos + 1 - start;
                    }
                    if (c == '.' || c == '[' || c == '(' || c == ')') {
                        return 0;
                    }
                    pos++;
                }
                return 0;
            default:
                return 0;
        }
    }

    /**
     * Returns the length of the valid method descriptor at the start of the given text, or 0 if there isn't one.
     */
    private static int methodDescriptorLength(CharSequence text) {
        if (text.length() == 0 || text.charAt(0) != '(') {
            return 0;
        }
        int pos = 1;
        while (pos < text.length() && text.charAt(pos) != ')') {
            int length = fieldDescriptorLength(text, pos);
            if (length == 0) {
                return 0;
            }
            pos += length;
        }
        if (pos == text.length()) {
            return 0;
        }
        pos++;
        if (pos < text.length() && text.charAt(pos) == 'V') {
            return pos + 1;
        }
        int length = fieldDescriptorLength(text, pos);
        return length == 0 ? 0 : pos + length;
    }

    /**
     * Lexes a descriptor. Descriptors may be directly followed by another token, so only the longest valid descriptor
     * is taken. If there is no valid descriptor, the whole word is taken and reported as invalid by the annotator.
     */
    private IElementType descriptor() {
        CharSequence text = yytext();
        int length = methodDescriptor ? methodDescriptorLength(text) : fieldDescriptorLength(text, 0);
        if (length != 0 && length < text.length()) {
            yypushback(text.length() - length);
        }
        yybegin(BODY);
        return methodDescriptor ? METHOD_DESCRIPTOR : FIELD_DESCRIPTOR;
    }

    private void beginMemberSignature(boolean method) {
        methodDescriptor = method;
        yybegin(MS_CLASS);
    }
%}

%public
%class UnpickLexer
%implements FlexLexer
%function advance
%type IElementType

%unicode

// The initial state is the start of a line. It is the only state the editor highlighter will restart lexing from, so
// no state other than the start of the line may carry over between lines.
%x HEADER_REST
%x UNSUPPORTED
%x BODY
%x AFTER_AT
%x AFTER_SCOPE
%x MS_CLASS
%x MS_CLASS_AFTER_IDENT
%x MS_ANGLE
%x MS_DESC

IDENTIFIER=[a-zA-Z_$][a-zA-Z0-9_$]*

DIGITS=[0-9]+
DOUBLE={DIGITS}\.{DIGITS}([eE][+-]?{DIGITS})?
FLOAT={DOUBLE}[fF]
INTEGER=[1-9][0-9]*|0[xX][0-9a-fA-F]+|0[bB][01]+|0[0-7]*
LONG={INTEGER}[lL]
// Anything that starts like a number but isn't one. Tokenized as a single token so the annotator can report a nice
// error, rather than the parser reporting a confusing one.
MALFORMED_NUMBER=[0-9][a-zA-Z0-9_$]*(\.([a-zA-Z0-9_$]*[eE][+-])?[a-zA-Z0-9_$]*)?

CHAR='([^'\\\r\n]|\\[^\r\n])*'?
STRING=\"([^\"\\\r\n]|\\[^\r\n])*\"?

DESCRIPTOR=[^ \t\r\n#]+

DOC_COMMENT=#:[^\r\n]*
COMMENT=#[^\r\n]*
NEWLINE=\r\n|\r|\n
SPACE=[ \t\f]+

%%

<YYINITIAL> {
    "unpick"                                    {
                                                    if (zzStartRead == 0) {
                                                        supportedVersion = true;
                                                        seenVersion = false;
                                                        yybegin(HEADER_REST);
                                                        return UNPICK_KW;
                                                    }
                                                    yybegin(BODY);
                                                    return IDENTIFIER;
                                                }
    "group"                                     { yybegin(BODY); return GROUP_KW; }
    "target_method"                             { beginMemberSignature(true); return TARGET_METHOD_KW; }
    "target_field"                              { beginMemberSignature(false); return TARGET_FIELD_KW; }
    "target_annotation"                         { yybegin(BODY); return TARGET_ANNOTATION_KW; }
    {IDENTIFIER}                                { yybegin(BODY); return IDENTIFIER; }

    // Whitespace on an otherwise blank line is ignored
    {SPACE} / [\r\n#]                           { return WHITE_SPACE; }
    {SPACE}                                     {
                                                    if (zzMarkedPos == zzEndRead) {
                                                        return WHITE_SPACE;
                                                    }
                                                    yybegin(BODY);
                                                    return INDENT;
                                                }
    {DOC_COMMENT}                               { return DOC_COMMENT; }
    {COMMENT}                                   { return COMMENT; }
    // Blank lines are ignored
    {NEWLINE}                                   { return WHITE_SPACE; }
    [^]                                         { yypushback(1); yybegin(BODY); }
}

<HEADER_REST> {
    {IDENTIFIER}                                {
                                                    if (!seenVersion) {
                                                        seenVersion = true;
                                                        String version = yytext().toString();
                                                        supportedVersion = version.equals("v3") || version.equals("v4");
                                                    }
                                                    return IDENTIFIER;
                                                }
    {NEWLINE}                                   {
                                                    yybegin(supportedVersion ? YYINITIAL : UNSUPPORTED);
                                                    return NEWLINE;
                                                }
}

<UNSUPPORTED> {
    [^]+                                        { return UNSUPPORTED_BODY; }
}

<AFTER_AT> {
    "scope"                                     { yybegin(AFTER_SCOPE); return IDENTIFIER; }
    {IDENTIFIER}                                { yybegin(BODY); return IDENTIFIER; }
}

<AFTER_SCOPE> {
    "method"                                    { beginMemberSignature(true); return IDENTIFIER; }
    {IDENTIFIER}                                { yybegin(BODY); return IDENTIFIER; }
}

<AFTER_AT, AFTER_SCOPE> {
    {SPACE}                                     { return WHITE_SPACE; }
    {NEWLINE}                                   { yybegin(YYINITIAL); return NEWLINE; }
    [^]                                         { yypushback(1); yybegin(BODY); }
}

<MS_CLASS> {
    {IDENTIFIER}                                { yybegin(MS_CLASS_AFTER_IDENT); return IDENTIFIER; }
}

<MS_CLASS_AFTER_IDENT> {
    "."                                         { yybegin(MS_CLASS); return DOT; }
    {IDENTIFIER}                                { yybegin(MS_DESC); return IDENTIFIER; }
    "<"                                         { yybegin(MS_ANGLE); return LT; }
}

<MS_ANGLE> {
    {IDENTIFIER}                                { return IDENTIFIER; }
    ">"                                         { yybegin(MS_DESC); return GT; }
}

<MS_DESC> {
    {DESCRIPTOR}                                { return descriptor(); }
}

<MS_CLASS, MS_CLASS_AFTER_IDENT, MS_ANGLE, MS_DESC> {
    {SPACE}                                     { return WHITE_SPACE; }
    {DOC_COMMENT}                               { return DOC_COMMENT; }
    {COMMENT}                                   { return COMMENT; }
    {NEWLINE}                                   { yybegin(YYINITIAL); return NEWLINE; }
    [^]                                         { yypushback(1); yybegin(BODY); }
}

<BODY> {
    {IDENTIFIER}                                { return IDENTIFIER; }

    {INTEGER}                                   { return INTEGER; }
    {LONG}                                      { return LONG; }
    {DOUBLE}                                    { return DOUBLE; }
    {FLOAT}                                     { return FLOAT; }
    {MALFORMED_NUMBER}                          { return MALFORMED_NUMBER; }

    {CHAR}                                      { return CHAR; }
    {STRING}                                    { return STRING; }

    "<<"                                        { return SHL; }
    ">>>"                                       { return USHR; }
    ">>"                                        { return SHR; }
    "<"                                         { return LT; }
    ">"                                         { return GT; }
    "|"                                         { return PIPE; }
    "^"                                         { return CARET; }
    "&"                                         { return AMPERSAND; }
    "+"                                         { return PLUS; }
    "-"                                         { return MINUS; }
    "*"                                         { return STAR; }
    "/"                                         { return SLASH; }
    "%"                                         { return PERCENT; }
    "~"                                         { return TILDE; }
    "("                                         { return LPAREN; }
    ")"                                         { return RPAREN; }
    "."                                         { return DOT; }
    ":"                                         { return COLON; }
    "@"                                         { yybegin(AFTER_AT); return AT; }
}

<HEADER_REST, BODY> {
    {SPACE}                                     { return WHITE_SPACE; }
    {DOC_COMMENT}                               { return DOC_COMMENT; }
    {COMMENT}                                   { return COMMENT; }
    {NEWLINE}                                   { yybegin(YYINITIAL); return NEWLINE; }
    [^]                                         { return BAD_CHARACTER; }
}
