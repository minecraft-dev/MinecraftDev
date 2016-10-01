package com.demonwav.mcdev.platform.forge.cfg.psi.mixins;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFieldName;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class CfgFieldNameMixin extends ASTWrapperPsiElement implements CfgFieldName {
    public CfgFieldNameMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setFieldName(String fieldName) {
        replace(CfgElementFactory.createFieldName(getProject(), fieldName));
    }

    public String getFieldNameText() {
        return getNameElement().getText();
    }
}
