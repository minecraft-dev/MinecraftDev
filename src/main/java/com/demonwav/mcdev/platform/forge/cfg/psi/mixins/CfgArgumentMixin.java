package com.demonwav.mcdev.platform.forge.cfg.psi.mixins;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgArgument;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

public abstract class CfgArgumentMixin extends ASTWrapperPsiElement implements CfgArgument {

    public CfgArgumentMixin(@NotNull ASTNode node) {
        super(node);
    }

    public PsiClass getArgumentClass() {
        return CfgMixinUtil.getClassFromString(getArgumentText(), getProject());
    }

    public String getArgumentText() {
        return getClassValue() != null ? getClassValue().getText() : getPrimitive() != null ? getPrimitive().getText() : null;
    }

    public void setArgument(String argument) {
        replace(CfgElementFactory.createArgument(getProject(), argument));
    }
}
