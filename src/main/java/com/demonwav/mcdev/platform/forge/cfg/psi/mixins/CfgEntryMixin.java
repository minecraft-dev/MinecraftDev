package com.demonwav.mcdev.platform.forge.cfg.psi.mixins;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgEntry;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFieldName;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class CfgEntryMixin extends ASTWrapperPsiElement implements CfgEntry {
    public CfgEntryMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setEntry(String entry) {
        replace(CfgElementFactory.createEntry(getProject(), entry));
    }

    public void setKeyword(CfgElementFactory.Keyword keyword) {
        getKeyword().replace(CfgElementFactory.createKeyword(getProject(), keyword));
    }

    public void setClassName(String className) {
        getClassName().replace(CfgElementFactory.createClassName(getProject(), className));
    }

    public void setFieldName(String fieldName) {
        final CfgFieldName newField = CfgElementFactory.createFieldName(getProject(), fieldName);
        // one of these must be true
        if (getFieldName() != null) {
            getFieldName().replace(newField);
        } else if (getFunction() != null) {
            getFunction().replace(newField);
        } else if (getAsterisk() != null) {
            getAsterisk().replace(newField);
        }
    }
}
