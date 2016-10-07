/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at;

import com.demonwav.mcdev.platform.mcp.at.psi.AtTypes;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class AtSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey KEYWORD =
        TextAttributesKey.createTextAttributesKey("AT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey CLASS_NAME =
        TextAttributesKey.createTextAttributesKey("AT_CLASS_NAME", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey CLASS_VALUE =
        TextAttributesKey.createTextAttributesKey("AT_CLASS_VALUE", DefaultLanguageHighlighterColors.STATIC_METHOD);
    public static final TextAttributesKey ELEMENT_NAME =
        TextAttributesKey.createTextAttributesKey("AT_ELEMENT_NAME", DefaultLanguageHighlighterColors.STATIC_FIELD);
    public static final TextAttributesKey ASTERISK =
        TextAttributesKey.createTextAttributesKey("AT_ASTERISK", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey PRIMITIVE =
        TextAttributesKey.createTextAttributesKey("AT_PRIMITIVE", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey COMMENT =
        TextAttributesKey.createTextAttributesKey("AT_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BAD_CHARACTER =
        TextAttributesKey.createTextAttributesKey("AT_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

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
        return new AtLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(AtTypes.KEYWORD_ELEMENT)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(AtTypes.CLASS_NAME_ELEMENT)) {
            return CLASS_NAME_KEYS;
        } else if (tokenType.equals(AtTypes.CLASS_VALUE)) {
            return CLASS_VALUE_KEYS;
        } else if (tokenType.equals(AtTypes.NAME_ELEMENT)) {
            return ELEMENT_NAME_KEYS;
        } else if (tokenType.equals(AtTypes.ASTERISK_ELEMENT)) {
            return ASTERISK_KEYS;
        } else if (tokenType.equals(AtTypes.PRIMITIVE)) {
            return PRIMITIVE_KEYS;
        } else if (tokenType.equals(AtTypes.COMMENT)) {
            return COMMENT_KEYS;
        } else if (tokenType.equals(TokenType.BAD_CHARACTER)) {
            return BAD_CHARACTER_KEYS;
        } else {
            return EMPTY_KEYS;
        }
    }
}
