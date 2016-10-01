package com.demonwav.mcdev.platform.forge.cfg.psi.mixins;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFunction;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class CfgFunctionMixin extends ASTWrapperPsiElement implements CfgFunction {

    public CfgFunctionMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setArgumentList(String arguments) {
        final String funcName = getFuncName().getNameElement().getText();
        getArgumentList().forEach(PsiElement::delete);
        //noinspection ConstantConditions
        final String returnValue = getReturnValue().getClassValue().getText();
        replace(CfgElementFactory.createFunction(getProject(), funcName + "(" + arguments + ")" + returnValue));
    }

    public void setReturnValue(String returnValue) {
        getReturnValue().replace(CfgElementFactory.createReturnValue(getProject(), returnValue));
    }

    public void setFunction(String function) {
        replace(CfgElementFactory.createFunction(getProject(), function));
    }
}
