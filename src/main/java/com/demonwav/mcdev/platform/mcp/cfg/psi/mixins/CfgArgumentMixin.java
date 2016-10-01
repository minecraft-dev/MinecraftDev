package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CfgArgumentMixin extends PsiElement {

    @Nullable
    PsiElement getClassValue();

    @Nullable
    PsiElement getPrimitive();

    @Nullable
    PsiClass getArgumentClass();

    @NotNull
    String getArgumentText();

    void setArgument(@NotNull String argument);
}
