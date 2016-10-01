package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.CfgClassNameMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CfgClassNameImplMixin extends ASTWrapperPsiElement implements CfgClassNameMixin {

    public CfgClassNameImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public PsiClass getClassNameValue() {
        return JavaPsiFacade.getInstance(getProject()).findClass(getClassNameText(), GlobalSearchScope.allScope(getProject()));
    }

    @NotNull
    public String getClassNameText() {
        return getClassNameElement().getText();
    }

    public void setClassName(@NotNull String className) {
        replace(CfgElementFactory.createClassName(getProject(), className));
    }
}
