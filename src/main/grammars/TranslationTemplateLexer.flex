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

package com.demonwav.mcdev.translations.template.gen;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.demonwav.mcdev.translations.lang.gen.psi.LangTypes.*;
import static com.intellij.psi.TokenType.*;

%%

%{
    public TranslationTemplateLexer() {
        this((java.io.Reader)null);
    }
%}

%public
%class TranslationTemplateLexer
%implements FlexLexer
%function advance
%type IElementType

%s WAITING_VALUE
%s WAITING_EQUALS

%unicode

EOL_WS              = \n | \r | \r\n
LINE_ENDING         = {EOL_WS}+

KEY = [^#\n\r][^\n\r]*
COMMENT = #[^\n\r]*

%%

<YYINITIAL> {
    {KEY}                       { return KEY; }
    {COMMENT}                   { return COMMENT; }
    {LINE_ENDING}               { return LINE_ENDING; }
}

[^]                             { return BAD_CHARACTER; }
