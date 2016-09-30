package com.demonwav.mcdev.platform.forge.cfg;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CfgLexer extends Lexer {
    private int state;
    private int tokenStart = 0;
    private int tokenEnd;
    private CharSequence bufferSequence;
    private int bufferEnd;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.bufferSequence = buffer;
        this.bufferEnd = endOffset;
        this.state = initialState;
        this.tokenEnd = startOffset;

        advance();
    }

    private int findNewToken(int tokenStart) {
        return 0;
    }

    private void setNewToken() {

    }

    @Override
    public int getState() {
        return state;
    }

    @Nullable
    @Override
    public IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        tokenStart = tokenEnd;
        tokenEnd = findNewToken(tokenStart);
        setNewToken();
    }

    @NotNull
    @Override
    public LexerPosition getCurrentPosition() {
        return new LexerPosition() {
            @Override
            public int getOffset() {
                return tokenEnd;
            }

            @Override
            public int getState() {
                return state;
            }
        };
    }

    @Override
    public void restore(@NotNull LexerPosition position) {
        this.tokenEnd = position.getOffset();
        this.state = position.getState();
    }

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return bufferSequence;
    }

    @Override
    public int getBufferEnd() {
        return bufferEnd;
    }
}
