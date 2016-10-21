/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.inspections;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mcp.util.McpConstants;
import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class EntityConstructorInspection extends BaseInspection {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "MCP Entity class missing World constructor";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        return "All Events must have a constructor that takes one " + McpConstants.WORLD + " parameter.";
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitClass(PsiClass aClass) {
                if (!McPsiUtil.extendsOrImplementsClass(aClass, McpConstants.ENTITY)) {
                    return;
                }

                final Module module = ModuleUtilCore.findModuleForPsiElement(aClass);
                if (module == null) {
                    return;
                }

                final MinecraftModule instance = MinecraftModule.getInstance(module);
                if (instance == null) {
                    return;
                }

                if (!instance.isOfType(McpModuleType.getInstance())) {
                    return;
                }

                final PsiMethod[] constructors = aClass.getConstructors();
                for (PsiMethod constructor : constructors) {
                    if (constructor.getParameterList().getParameters().length != 1) {
                        continue;
                    }

                    final PsiParameter parameter = constructor.getParameterList().getParameters()[0];
                    final PsiTypeElement typeElement = parameter.getTypeElement();
                    if (typeElement == null) {
                        continue;
                    }

                    final PsiType type = typeElement.getType();
                    if (!(type instanceof PsiClassType)) {
                        continue;
                    }

                    final PsiClass resolve = ((PsiClassType) type).resolve();
                    if (resolve == null) {
                        continue;
                    }

                    if (resolve.getQualifiedName() == null) {
                        continue;
                    }

                    if (resolve.getQualifiedName().equals(McpConstants.WORLD)) {
                        return;
                    }
                }

                registerClassError(aClass);
            }
        };
    }
}
