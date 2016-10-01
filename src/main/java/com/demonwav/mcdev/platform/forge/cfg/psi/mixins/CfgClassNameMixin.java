package com.demonwav.mcdev.platform.forge.cfg.psi.mixins;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgClassName;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public abstract class CfgClassNameMixin extends ASTWrapperPsiElement implements CfgClassName {

    public CfgClassNameMixin(@NotNull ASTNode node) {
        super(node);
    }

    public PsiClass getClassNameValue() {
        return JavaPsiFacade.getInstance(getProject()).findClass(getClassNameText(), GlobalSearchScope.allScope(getProject()));
    }

    public String getClassNameText() {
        return getClassNameElement().getText();
    }

    public void setClassName(String className) {
        replace(CfgElementFactory.createClassName(getProject(), className));
    }
}
