package com.demonwav.mcdev.platform.forge.cfg;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class CfgSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey KEYWORD =
        TextAttributesKey.createTextAttributesKey("CFG_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey CLASS_NAME =
        TextAttributesKey.createTextAttributesKey("CFG_CLASS_NAME", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey CLASS_VALUE =
        TextAttributesKey.createTextAttributesKey("CFG_CLASS_VALUE", DefaultLanguageHighlighterColors.STATIC_METHOD);
    public static final TextAttributesKey ELEMENT_NAME =
        TextAttributesKey.createTextAttributesKey("CFG_ELEMENT_NAME", DefaultLanguageHighlighterColors.STATIC_FIELD);
    public static final TextAttributesKey ASTERISK =
        TextAttributesKey.createTextAttributesKey("CFG_ASTERISK", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey PRIMITIVE =
        TextAttributesKey.createTextAttributesKey("CFG_PRIMITIVE", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey COMMENT =
        TextAttributesKey.createTextAttributesKey("CFG_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BAD_CHARACTER =
        TextAttributesKey.createTextAttributesKey("CFG_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[]{KEYWORD};
    private static final TextAttributesKey[] CLASS_NAME_KEYS = new TextAttributesKey[]{CLASS_NAME};
    private static final TextAttributesKey[] CLASS_VALUE_KEYS = new TextAttributesKey[]{CLASS_VALUE};
    private static final TextAttributesKey[] ELEMENT_NAME_KEYS = new TextAttributesKey[]{ELEMENT_NAME};
    private static final TextAttributesKey[] ASTERISK_KEYS = new TextAttributesKey[]{ASTERISK};
    private static final TextAttributesKey[] PRIMITIVE_KEYS = new TextAttributesKey[]{PRIMITIVE};
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] BAD_CHARACTER_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new CfgLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(CfgTypes.KEYWORD_ELEMENT)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(CfgTypes.CLASS_NAME_ELEMENT)) {
            return CLASS_NAME_KEYS;
        } else if (tokenType.equals(CfgTypes.CLASS_VALUE)) {
            return CLASS_VALUE_KEYS;
        } else if (tokenType.equals(CfgTypes.NAME_ELEMENT)) {
            return ELEMENT_NAME_KEYS;
        } else if (tokenType.equals(CfgTypes.ASTERISK)) {
            return ASTERISK_KEYS;
        } else if (tokenType.equals(CfgTypes.PRIMITIVE)) {
            return PRIMITIVE_KEYS;
        } else if (tokenType.equals(CfgTypes.COMMENT)) {
            return COMMENT_KEYS;
        } else if (tokenType.equals(TokenType.BAD_CHARACTER)) {
            return BAD_CHARACTER_KEYS;
        } else {
            return EMPTY_KEYS;
        }
    }
}
