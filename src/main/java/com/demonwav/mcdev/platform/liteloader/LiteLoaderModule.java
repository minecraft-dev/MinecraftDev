package com.demonwav.mcdev.platform.liteloader;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class LiteLoaderModule extends AbstractModule {

    LiteLoaderModule(@NotNull Module module) {
        super(module);
        this.buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            buildSystem.reImport(module);
        }
    }

    @Override
    public GradleBuildSystem getBuildSystem() {
        return (GradleBuildSystem) buildSystem;
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
        return true;
    }

    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "";
    }

    @Override
    public boolean shouldShowPluginIcon(@Nullable PsiElement element) {
        return element instanceof PsiIdentifier &&
            element.getParent() instanceof PsiClass &&
            element.getText().startsWith("LiteMod");
    }
}
