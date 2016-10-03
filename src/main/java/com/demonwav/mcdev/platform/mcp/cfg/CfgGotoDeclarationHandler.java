package com.demonwav.mcdev.platform.mcp.cfg;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mcp.McpModule;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgClassName;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgEntry;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgFieldName;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgFuncName;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgFunction;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgTypes;
import com.demonwav.mcdev.platform.mcp.srg.SrgManager;
import com.demonwav.mcdev.platform.mcp.srg.SrgMap;
import com.demonwav.mcdev.platform.mcp.util.McpUtil;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
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

        final Module module = ModuleUtilCore.findModuleForPsiElement(sourceElement);
        if (module == null) {
            return null;
        }

        final MinecraftModule instance = MinecraftModule.getInstance(module);
        if (instance == null) {
            return null;
        }

        final McpModule mcpModule = instance.getModuleOfType(McpModuleType.getInstance());
        if (mcpModule == null) {
            return null;
        }

        // Recompute if we don't already have it just in case
        SrgManager.getInstance(mcpModule).recomputeIfNullAndGetSrgMap();
        final SrgMap srgMap = SrgManager.getInstance(mcpModule).getSrgMapNow();
        if (srgMap == null) {
            return null;
        }

        if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.CLASS_NAME) {
            final CfgClassName className = (CfgClassName) sourceElement.getParent();
            final String classSrgToMcp = srgMap.findClassSrgToMcp(McpUtil.replaceDotWithSlash(className.getClassNameText()));
            final PsiClass psiClass = SrgMap.fromClassString(classSrgToMcp, sourceElement.getProject());
            if (psiClass == null) {
                return null;
            }
            return new PsiElement[]{psiClass};
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.FUNC_NAME) {
            final CfgFuncName funcName = (CfgFuncName) sourceElement.getParent();
            final CfgFunction function = (CfgFunction) funcName.getParent();
            final CfgEntry entry = (CfgEntry) function.getParent();
            final CfgClassName className = entry.getClassName();

            if (funcName.getFuncNameText().equals("<init>")) {
                final String classSrgToMcp = srgMap.findClassSrgToMcp(McpUtil.replaceDotWithSlash(className.getText()));
                final PsiClass psiClass = SrgMap.fromClassString(classSrgToMcp, sourceElement.getProject());
                if (psiClass == null) {
                    return null;
                }

                final PsiMethod method = SrgMap.fromConstructorString(function.getText(), psiClass);
                if (method == null) {
                    return null;
                }

                return new PsiElement[]{method};
            } else {
                final String methodSrgToMcp = srgMap.findMethodSrgToMcp(McpUtil.replaceDotWithSlash(className.getText()) + "/" + function.getText());
                final PsiMethod psiMethod = SrgMap.fromMethodString(methodSrgToMcp, sourceElement.getProject());
                if (psiMethod == null) {
                    return null;
                }
                return new PsiElement[]{psiMethod};
            }
        } else if (sourceElement.getNode().getTreeParent().getElementType() == CfgTypes.FIELD_NAME) {
            final CfgFieldName fieldName = (CfgFieldName) sourceElement.getParent();
            final CfgEntry entry = (CfgEntry) fieldName.getParent();
            final CfgClassName className = entry.getClassName();

            final String fieldSrgToMcp = srgMap.findFieldSrgToMcp(McpUtil.replaceDotWithSlash(className.getText()) + "/" + fieldName.getFieldNameText());
            final PsiField psiField = SrgMap.fromFieldString(fieldSrgToMcp, sourceElement.getProject());
            if (psiField == null) {
                return null;
            }
            return new PsiElement[]{psiField};
        } else if (sourceElement.getNode().getElementType() == CfgTypes.CLASS_VALUE) {
            String normalized = McpUtil.normalizeClassString(sourceElement.getText());

            // unlike the others, this isn't necessary srg mapped
            PsiClass psiClass = JavaPsiFacade.getInstance(sourceElement.getProject()).findClass(McpUtil.replaceSlashWithDot(normalized),
                GlobalSearchScope.allScope(sourceElement.getProject()));
            if (psiClass != null) {
                return new PsiElement[]{psiClass};
            }


            final String classSrgToMcp = srgMap.findClassSrgToMcp(normalized);
            psiClass = SrgMap.fromClassString(classSrgToMcp, sourceElement.getProject());
            if (psiClass == null) {
                return null;
            }
            return new PsiElement[]{psiClass};
        } else if (sourceElement.getNode().getElementType() == CfgTypes.PRIMITIVE) {
            String text = sourceElement.getText();

            final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(sourceElement.getProject());
            final Project project = sourceElement.getProject();
            final PsiClass psiClass;
            switch (text) {
                case "B":
                    psiClass = javaPsiFacade.findClass(Byte.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                    break;
                case "C":
                    psiClass = javaPsiFacade.findClass(Character.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                    break;
                case "D":
                    psiClass = javaPsiFacade.findClass(Double.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                    break;
                case "F":
                    psiClass = javaPsiFacade.findClass(Float.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                    break;
                case "I":
                    psiClass = javaPsiFacade.findClass(Integer.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                    break;
                case "J":
                    psiClass = javaPsiFacade.findClass(Long.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                    break;
                case "S":
                    psiClass = javaPsiFacade.findClass(Short.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                    break;
                case "Z":
                    psiClass = javaPsiFacade.findClass(Boolean.class.getCanonicalName(), GlobalSearchScope.allScope(project));
                    break;
                default:
                    return null;
            }
            if (psiClass == null) {
                return null;
            }
            return new PsiElement[]{psiClass};
        }

        return null;
    }

    @Nullable
    @Override
    public String getActionText(DataContext context) {
        return null;
    }
}
