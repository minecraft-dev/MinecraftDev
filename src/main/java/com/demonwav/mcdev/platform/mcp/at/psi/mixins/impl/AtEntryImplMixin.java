/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi.mixins.impl;

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtAsterisk;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction;
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtEntryMixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class AtEntryImplMixin extends ASTWrapperPsiElement implements AtEntryMixin {
    public AtEntryImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public void setEntry(@NotNull String entry) {
        replace(AtElementFactory.createEntry(getProject(), entry));
    }

    public void setKeyword(@NotNull AtElementFactory.Keyword keyword) {
        getKeyword().replace(AtElementFactory.createKeyword(getProject(), keyword));
    }

    public void setClassName(@NotNull String className) {
        getClassName().replace(AtElementFactory.createClassName(getProject(), className));
    }

    public void setFieldName(@NotNull String fieldName) {
        final AtFieldName newField = AtElementFactory.createFieldName(getProject(), fieldName);
        replaceMember(newField);
    }

    public void setFunction(@NotNull String functionText) {
        final AtFunction function = AtElementFactory.createFunction(getProject(), functionText);
        replaceMember(function);
    }

    public void setAsterisk() {
        final AtAsterisk asterisk = AtElementFactory.createAsterisk(getProject());
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
