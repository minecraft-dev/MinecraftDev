package com.demonwav.mcdev.platform.mcp.at.psi.mixins;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface AtFuncNameMixin extends PsiElement {

    @NotNull
    PsiElement getNameElement();

    void setFuncName(@NotNull String funcName);

    @NotNull
    String getFuncNameText();
}
