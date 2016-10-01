package com.demonwav.mcdev.platform.forge.cfg;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.*;
import static com.intellij.psi.TokenType.*;

%%

%{
  public CfgLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class CfgLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

PRIMITIVE=[ZBCSIFDJV]
CLASS_VALUE=\[*([ZBCSIFDJ]|L[^;]+;)
KEYWORD_ELEMENT=(public|public-f|private|private-f|protected|protected-f)
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
    "*"                                         { return ASTERISK_ELEMENT; }

    {PRIMITIVE} ({PRIMITIVE}|{CLASS_VALUE})*    { zzMarkedPos = zzStartRead + 1; return PRIMITIVE; }

    {CLASS_VALUE}                               { return CLASS_VALUE; }
    {KEYWORD_ELEMENT}                           { return KEYWORD_ELEMENT; }
    {NAME_ELEMENT}                              { return NAME_ELEMENT; }
    {CLASS_NAME_ELEMENT}                        { return CLASS_NAME_ELEMENT; }
    {COMMENT}                                   { return COMMENT; }
}

[^]                                             { return BAD_CHARACTER; }
