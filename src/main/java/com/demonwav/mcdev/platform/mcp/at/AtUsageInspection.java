package com.demonwav.mcdev.platform.mcp.at;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mcp.McpModule;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry;
import com.demonwav.mcdev.platform.mcp.srg.SrgManager;
import com.demonwav.mcdev.platform.mcp.srg.SrgMap;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AtUsageInspection extends LocalInspectionTool {

    @Nullable
    @Override
    public String getStaticDescription() {
        return "The declared access transformer is never used";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (!(element instanceof AtEntry)) {
                    return;
                }

                final Module module = ModuleUtilCore.findModuleForPsiElement(element);
                if (module == null) {
                    return;
                }

                final MinecraftModule instance = MinecraftModule.getInstance(module);
                if (instance == null) {
                    return;
                }

                final McpModule mcpModule = instance.getModuleOfType(McpModuleType.getInstance());
                if (mcpModule == null) {
                    return;
                }

                AtEntry entry = (AtEntry) element;

                PsiClass psiClass = entry.getClassName().getClassNameValue();
                if (psiClass == null) {
                    // Ignore here, we'll flag this as an error in another inspection
                    return;
                }

                SrgManager.getInstance(mcpModule).recomputeIfNullAndGetSrgMap().done(srgMap -> {
                    // TODO
                });
            }
        };
    }
}
