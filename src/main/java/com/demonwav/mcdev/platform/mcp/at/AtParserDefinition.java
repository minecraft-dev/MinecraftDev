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

import com.demonwav.mcdev.platform.mcp.at.parser.AtParser;
import com.demonwav.mcdev.platform.mcp.at.psi.AtTypes;

import com.google.common.collect.ImmutableMap;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AtParserDefinition implements ParserDefinition {
    private static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    private static final TokenSet COMMENTS = TokenSet.create(AtTypes.COMMENT);

    private static final IFileElementType FILE = new IFileElementType(Language.findInstance(AtLanguage.class));

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new AtLexerAdapter();
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    @Override
    public PsiParser createParser(Project project) {
        return new AtParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new AtFile(viewProvider);
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return map.entrySet().stream()
            .filter(e -> left.getElementType().equals(e.getKey().getFirst()) || right.getElementType().equals(e.getKey().getSecond()))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse(SpaceRequirements.MUST_NOT);
    }

    private static final Map<Pair<IElementType, IElementType>, SpaceRequirements> map =
        ImmutableMap.<Pair<IElementType, IElementType>, SpaceRequirements>builder()
            .put(Pair.create(AtTypes.KEYWORD, AtTypes.CLASS_NAME), SpaceRequirements.MUST)
            .put(Pair.create(AtTypes.CLASS_NAME, AtTypes.FIELD_NAME), SpaceRequirements.MUST)
            .put(Pair.create(AtTypes.CLASS_NAME, AtTypes.FUNCTION), SpaceRequirements.MUST)
            .put(Pair.create(AtTypes.CLASS_NAME, AtTypes.ASTERISK), SpaceRequirements.MUST)
            .put(Pair.create(AtTypes.FIELD_NAME, AtTypes.COMMENT), SpaceRequirements.MUST)
            .put(Pair.create(AtTypes.ASTERISK, AtTypes.COMMENT), SpaceRequirements.MUST)
            .put(Pair.create(AtTypes.COMMENT, AtTypes.KEYWORD), SpaceRequirements.MUST_LINE_BREAK)
            .put(Pair.create(AtTypes.COMMENT, AtTypes.COMMENT), SpaceRequirements.MUST_LINE_BREAK)
            .put(Pair.create(AtTypes.FUNCTION, AtTypes.COMMENT), SpaceRequirements.MUST)
            .build();

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return AtTypes.Factory.createElement(node);
    }
}
