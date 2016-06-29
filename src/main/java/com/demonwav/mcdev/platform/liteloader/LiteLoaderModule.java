package com.demonwav.mcdev.platform.liteloader;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class LiteLoaderModule extends AbstractModule {

    LiteLoaderModule(@NotNull Module module) {
        this.module = module;
        this.buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            if (!buildSystem.isImported()) {
                buildSystem.reImport(module);
            }
        }
    }

    @Override
    public LiteLoaderModuleType getModuleType() {
        return LiteLoaderModuleType.getInstance();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.LITELOADER;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.LITELOADER_ICON;
    }

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        // TODO
        return true;
    }

    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        // TODO
        return "";
    }
}
