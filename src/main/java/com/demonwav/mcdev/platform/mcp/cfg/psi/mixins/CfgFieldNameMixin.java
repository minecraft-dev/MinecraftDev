package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface CfgFieldNameMixin extends PsiElement {

    @NotNull
    PsiElement getNameElement();

    void setFieldName(@NotNull String fieldName);

    @NotNull
    String getFieldNameText();
}
