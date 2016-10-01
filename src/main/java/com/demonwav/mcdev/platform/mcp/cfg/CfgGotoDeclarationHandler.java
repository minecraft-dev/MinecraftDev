package com.demonwav.mcdev.platform.mcp.cfg;

import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgArgument;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgClassName;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgEntry;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgFieldName;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgReturnValue;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgTypes;
import com.demonwav.mcdev.platform.mcp.cfg.psi.impl.CfgFuncNameImpl;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.Nullable;

public class CfgGotoDeclarationHandler implements GotoDeclarationHandler {
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
        if (sourceElement == null) {
            return null;
        }

        if (sourceElement.getLanguage() != CfgLanguage.getInstance()) {
            return null;
        }

        if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.CLASS_NAME) {
            final PsiClass target = ((CfgClassName) sourceElement.getParent()).getClassNameValue();
            if (target == null) {
                return null;
            }
            return new PsiElement[]{target};
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.FUNC_NAME) {
            final String functionName = ((CfgFuncNameImpl) sourceElement.getParent()).getFuncNameText();
            final PsiClass target = ((CfgEntry) sourceElement.getParent().getParent().getParent()).getClassName().getClassNameValue();
            if (target == null) {
                return null;
            }
            return target.findMethodsByName(functionName, true);
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.FIELD_NAME) {
            final String fieldName = ((CfgFieldName) sourceElement.getParent()).getFieldNameText();
            final PsiClass target = ((CfgEntry) sourceElement.getParent().getParent()).getClassName().getClassNameValue();
            if (target == null) {
                return null;
            }

            final PsiField fieldByName = target.findFieldByName(fieldName, true);
            if (fieldByName == null) {
                return null;
            }

            return new PsiElement[]{fieldByName};
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.ARGUMENT) {
            final PsiClass target = ((CfgArgument) sourceElement.getParent()).getArgumentClass();
            if (target == null) {
                return null;
            }
            return new PsiElement[]{target};
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.RETURN_VALUE) {
            final PsiClass target = ((CfgReturnValue) sourceElement.getParent()).getReturnValueClass();
            if (target == null) {
                return null;
            }
            return new PsiElement[]{target};
        }

        return null;
    }

    @Nullable
    @Override
    public String getActionText(DataContext context) {
        return null;
    }
}
