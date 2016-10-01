package com.demonwav.mcdev.platform.forge.cfg.psi.mixins;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgKeyword;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class CfgKeywordMixin extends ASTWrapperPsiElement implements CfgKeyword {
    public CfgKeywordMixin(@NotNull ASTNode node) {
        super(node);
    }

    public CfgElementFactory.Keyword getKeywordValue() {
        return CfgElementFactory.Keyword.match(getKeywordElement().getText());
    }

    public void setKeyword(CfgElementFactory.Keyword keyword) {
        replace(CfgElementFactory.createKeyword(getProject(), keyword));
    }
}
