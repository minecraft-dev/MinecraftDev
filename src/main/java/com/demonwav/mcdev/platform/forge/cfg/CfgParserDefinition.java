package com.demonwav.mcdev.platform.forge.cfg;

import com.demonwav.mcdev.platform.forge.cfg.parser.CfgParser;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes;

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

public class CfgParserDefinition implements ParserDefinition {
    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    public static final TokenSet COMMENTS = TokenSet.create(CfgTypes.COMMENT);

    public static final IFileElementType FILE = new IFileElementType(Language.findInstance(CfgLanguage.class));

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new CfgLexerAdapter();
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
        return new CfgParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new CfgFile(viewProvider);
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
            .put(Pair.create(CfgTypes.KEYWORD, CfgTypes.CLASS_NAME), SpaceRequirements.MUST)
            .put(Pair.create(CfgTypes.CLASS_NAME, CfgTypes.FIELD_NAME), SpaceRequirements.MUST)
            .put(Pair.create(CfgTypes.CLASS_NAME, CfgTypes.FUNCTION), SpaceRequirements.MUST)
            .put(Pair.create(CfgTypes.CLASS_NAME, CfgTypes.ASTERISK), SpaceRequirements.MUST)
            .put(Pair.create(CfgTypes.FIELD_NAME, CfgTypes.COMMENT), SpaceRequirements.MUST)
            .put(Pair.create(CfgTypes.ASTERISK, CfgTypes.COMMENT), SpaceRequirements.MUST)
            .put(Pair.create(CfgTypes.COMMENT, CfgTypes.KEYWORD), SpaceRequirements.MUST_LINE_BREAK)
            .put(Pair.create(CfgTypes.FUNCTION, CfgTypes.COMMENT), SpaceRequirements.MUST)
            .build();

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return CfgTypes.Factory.createElement(node);
    }
}
