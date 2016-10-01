package com.demonwav.mcdev.platform.forge.cfg;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgArgument;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgClassName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgEntry;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFieldName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFuncName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgReturnValue;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
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
            final String className = ((CfgClassName) sourceElement.getParent()).getClassNameText();
            final PsiClass target = JavaPsiFacade.getInstance(sourceElement.getProject()).findClass(className, GlobalSearchScope.allScope(sourceElement.getProject()));
            if (target == null) {
                return null;
            }
            return new PsiElement[]{target};
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.FUNC_NAME) {
            final String functionName = ((CfgFuncName) sourceElement.getParent()).getFuncNameText();
            final String className = ((CfgEntry) sourceElement.getParent().getParent().getParent()).getClassName().getClassNameText();
            final PsiClass target = JavaPsiFacade.getInstance(sourceElement.getProject()).findClass(className, GlobalSearchScope.allScope(sourceElement.getProject()));
            if (target == null) {
                return null;
            }

            return target.findMethodsByName(functionName, true);
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.FIELD_NAME) {
            final String fieldName = ((CfgFieldName) sourceElement.getParent()).getFieldNameText();
            final String className = ((CfgEntry) sourceElement.getParent().getParent()).getClassName().getClassNameText();
            final PsiClass target = JavaPsiFacade.getInstance(sourceElement.getProject()).findClass(className, GlobalSearchScope.allScope(sourceElement.getProject()));
            if (target == null) {
                return null;
            }

            final PsiField fieldByName = target.findFieldByName(fieldName, true);
            if (fieldByName == null) {
                return null;
            }

            return new PsiElement[]{fieldByName};
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.ARGUMENT) {
            String argumentText = ((CfgArgument) sourceElement.getParent()).getArgumentText();
            return handleTypeString(argumentText, sourceElement);
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.RETURN_VALUE) {
            String returnText = ((CfgReturnValue) sourceElement.getParent()).getReturnValueText();
            return handleTypeString(returnText, sourceElement);
        }

        return null;
    }

    private PsiElement[] handleTypeString(String text, PsiElement element) {
        if (text.length() == 1) {
            final JavaPsiFacade instance = JavaPsiFacade.getInstance(element.getProject());
            switch (text) {
                case "B":
                    return new PsiElement[]{instance.findClass(Byte.class.getCanonicalName(), GlobalSearchScope.allScope(element.getProject()))};
                case "C":
                    return new PsiElement[]{instance.findClass(Character.class.getCanonicalName(), GlobalSearchScope.allScope(element.getProject()))};
                case "D":
                    return new PsiElement[]{instance.findClass(Double.class.getCanonicalName(), GlobalSearchScope.allScope(element.getProject()))};
                case "F":
                    return new PsiElement[]{instance.findClass(Float.class.getCanonicalName(), GlobalSearchScope.allScope(element.getProject()))};
                case "I":
                    return new PsiElement[]{instance.findClass(Integer.class.getCanonicalName(), GlobalSearchScope.allScope(element.getProject()))};
                case "J":
                    return new PsiElement[]{instance.findClass(Long.class.getCanonicalName(), GlobalSearchScope.allScope(element.getProject()))};
                case "S":
                    return new PsiElement[]{instance.findClass(Short.class.getCanonicalName(), GlobalSearchScope.allScope(element.getProject()))};
                case "Z":
                    return new PsiElement[]{instance.findClass(Boolean.class.getCanonicalName(), GlobalSearchScope.allScope(element.getProject()))};
                default:
                    return null;
            }
        } else {
            if (text.startsWith("L")) {
                text = text.substring(1);
            }

            // Remove array stuff and ; ending character
            text = text.replaceAll("\\[|;", "");
            // Change path to dot
            text = text.replaceAll("/", ".");

            PsiElement target = JavaPsiFacade.getInstance(element.getProject()).findClass(text, GlobalSearchScope.allScope(element.getProject()));
            if (target == null) {
                return null;
            }
            return new PsiElement[]{target};
        }
    }

    @Nullable
    @Override
    public String getActionText(DataContext context) {
        return null;
    }
}
