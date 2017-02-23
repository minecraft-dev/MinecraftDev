/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mcp.McpModule;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtClassName;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFuncName;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes;
import com.demonwav.mcdev.platform.mcp.srg.McpSrgMap;
import com.demonwav.mcdev.util.McPsiClass;
import com.demonwav.mcdev.util.MemberReference;
import com.demonwav.mcdev.util.PsiBytecodeUtil;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;

public class AtGotoDeclarationHandler implements GotoDeclarationHandler {
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
        if (sourceElement == null) {
            return null;
        }

        if (sourceElement.getLanguage() != AtLanguage.getInstance()) {
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

        final McpModule mcpModule = instance.getModuleOfType(McpModuleType.INSTANCE);
        if (mcpModule == null) {
            return null;
        }

        final McpSrgMap srgMap = mcpModule.getSrgManager().getSrgMapNow();
        if (srgMap == null) {
            return null;
        }

        if (sourceElement.getNode().getTreeParent().getElementType() == AtTypes.CLASS_NAME) {
            final AtClassName className = (AtClassName) sourceElement.getParent();
            String classSrgToMcp = srgMap.mapToMcpClass(className.getClassNameText());
            final PsiClass psiClass = McPsiClass.findQualifiedClass(sourceElement.getProject(), classSrgToMcp);
            return new PsiElement[]{psiClass};
        } else if (sourceElement.getNode().getTreeParent().getElementType() == AtTypes.FUNC_NAME) {
            final AtFuncName funcName = (AtFuncName) sourceElement.getParent();
            final AtFunction function = (AtFunction) funcName.getParent();
            final AtEntry entry = (AtEntry) function.getParent();

            MemberReference reference = srgMap.mapToMcpMethod(AtMemberReference.get(entry, function));
            final PsiMember member = reference.resolveMember(sourceElement.getProject());
            if (member == null) {
                return null;
            }
            return new PsiElement[]{member};
        } else if (sourceElement.getNode().getTreeParent().getElementType() == AtTypes.FIELD_NAME) {
            final AtFieldName fieldName = (AtFieldName) sourceElement.getParent();
            final AtEntry entry = (AtEntry) fieldName.getParent();

            final MemberReference reference = srgMap.mapToMcpField(AtMemberReference.get(entry, fieldName));
            final PsiMember member = reference.resolveMember(sourceElement.getProject());
            if (member == null) {
                return null;
            }
            return new PsiElement[]{member};
        } else if (sourceElement.getNode().getElementType() == AtTypes.CLASS_VALUE) {
            String className = srgMap.mapToMcpClass(PsiBytecodeUtil.parseClassDescriptor(sourceElement.getText()));
            PsiClass psiClass = McPsiClass.findQualifiedClass(sourceElement.getProject(), className);
            if (psiClass == null) {
                return null;
            }
            return new PsiElement[]{psiClass};
        } else if (sourceElement.getNode().getElementType() == AtTypes.PRIMITIVE) {
            String text = sourceElement.getText();
            if (text.length() != 1) {
                return null;
            }

            PsiPrimitiveType type = PsiBytecodeUtil.getPrimitiveType(text.charAt(0));
            if (type == null) {
                return null;
            }

            String boxedType = type.getBoxedTypeName();
            if (boxedType == null) {
                return null;
            }

            final PsiClass psiClass = JavaPsiFacade.getInstance(sourceElement.getProject()).findClass(boxedType,
                    GlobalSearchScope.allScope(sourceElement.getProject()));
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
