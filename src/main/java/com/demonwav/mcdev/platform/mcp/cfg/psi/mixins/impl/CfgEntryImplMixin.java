package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgAsterisk;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgFieldName;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgFunction;
import com.demonwav.mcdev.platform.mcp.cfg.psi.mixins.CfgEntryMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class CfgEntryImplMixin extends ASTWrapperPsiElement implements CfgEntryMixin {
    public CfgEntryImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setEntry(@NotNull String entry) {
        replace(CfgElementFactory.createEntry(getProject(), entry));
    }

    public void setKeyword(@NotNull CfgElementFactory.Keyword keyword) {
        getKeyword().replace(CfgElementFactory.createKeyword(getProject(), keyword));
    }

    public void setClassName(@NotNull String className) {
        getClassName().replace(CfgElementFactory.createClassName(getProject(), className));
    }

    public void setFieldName(@NotNull String fieldName) {
        final CfgFieldName newField = CfgElementFactory.createFieldName(getProject(), fieldName);
        replaceMember(newField);
    }

    public void setFunction(@NotNull String functionText) {
        final CfgFunction function = CfgElementFactory.createFunction(getProject(), functionText);
        replaceMember(function);
    }

    public void setAsterisk() {
        final CfgAsterisk asterisk = CfgElementFactory.createAsterisk(getProject());
        replaceMember(asterisk);
    }

    private void replaceMember(PsiElement element) {
        // One of these must be true
        if (getFieldName() != null) {
            getFieldName().replace(element);
        } else if (getFunction() != null) {
            getFunction().replace(element);
        } else if (getAsterisk() != null) {
            getAsterisk().replace(element);
        }
    }
}
