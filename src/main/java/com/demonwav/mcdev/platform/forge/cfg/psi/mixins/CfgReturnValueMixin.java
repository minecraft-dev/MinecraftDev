package com.demonwav.mcdev.platform.forge.cfg.psi.mixins;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgReturnValue;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

public abstract class CfgReturnValueMixin extends ASTWrapperPsiElement implements CfgReturnValue {
    public CfgReturnValueMixin(@NotNull ASTNode node) {
        super(node);
    }

    public PsiClass getReturnValueClass() {
        return CfgMixinUtil.getClassFromString(getReturnValueText(), getProject());
    }

    public String getReturnValueText() {
        return getPrimitive() != null ? getPrimitive().getText() : getClassValue() != null ? getClassValue().getText() : null;
    }

    public void setReturnValue(String returnValue) {
        replace(CfgElementFactory.createReturnValue(getProject(), returnValue));
    }
}
