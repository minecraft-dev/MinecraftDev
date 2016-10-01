package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CfgClassNameMixin extends PsiElement {

    @NotNull
    PsiElement getClassNameElement();

    @Nullable
    PsiClass getClassNameValue();

    @NotNull
    String getClassNameText();

    void setClassName(@NotNull String className);
}
