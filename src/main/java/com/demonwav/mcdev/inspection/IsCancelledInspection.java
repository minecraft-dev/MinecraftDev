/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.inspection;

import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiMethodCallExpression;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class IsCancelledInspection extends BaseInspection {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Useless event is cancelled check";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        final IsCancelled useless = (IsCancelled) infos[0];
        return useless.getErrorString();
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        final IsCancelled useless = (IsCancelled) infos[0];
        return useless.getBuildFix();
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                final Module module = ModuleUtilCore.findModuleForPsiElement(expression);
                if (module == null) {
                    return;
                }

                final MinecraftModule instance = MinecraftModule.getInstance(module);
                if (instance == null) {
                    return;
                }

                Optional<IsCancelled> useless = instance.getModules().stream()
                        .map(m -> m.checkUselessCancelCheck(expression))
                        .filter(i -> i != null)
                        .findAny();
                if (!useless.isPresent()) {
                    return;
                }

                registerMethodCallError(expression, useless.get());
            }
        };
    }
}
