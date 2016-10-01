package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface CfgFuncNameMixin extends PsiElement {

    @NotNull
    PsiElement getNameElement();

    void setFuncName(@NotNull String funcName);

    @NotNull
    String getFuncNameText();
}
