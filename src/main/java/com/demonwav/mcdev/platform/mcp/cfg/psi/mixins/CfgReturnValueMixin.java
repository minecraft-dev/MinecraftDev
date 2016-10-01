package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CfgReturnValueMixin extends PsiElement {

    @Nullable
    PsiElement getClassValue();

    @Nullable
    PsiElement getPrimitive();

    @Nullable
    PsiClass getReturnValueClass();

    @NotNull
    String getReturnValueText();

    void setReturnValue(@NotNull String returnValue);
}
