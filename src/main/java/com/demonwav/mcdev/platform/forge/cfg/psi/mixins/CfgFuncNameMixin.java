package com.demonwav.mcdev.platform.forge.cfg.psi.mixins;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFuncName;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class CfgFuncNameMixin extends ASTWrapperPsiElement implements CfgFuncName {
    public CfgFuncNameMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setFuncName(String funcName) {
        replace(CfgElementFactory.createFuncName(getProject(), funcName));
    }

    public String getFuncNameText() {
        return getNameElement().getText();
    }
}
