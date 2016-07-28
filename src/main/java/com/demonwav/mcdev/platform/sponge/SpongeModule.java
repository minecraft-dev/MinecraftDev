package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

public class SpongeModule extends AbstractModule {

    SpongeModule(@NotNull Module module) {
        super(module);
        buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            if (!buildSystem.isImported()) {
                buildSystem.reImport(module);
            }
        }
    }

    @NotNull
    public Module getModule() {
        return module;
    }

    @Override
    public AbstractModuleType<SpongeModule> getModuleType() {
        return SpongeModuleType.getInstance();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.SPONGE;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.SPONGE_ICON;
    }

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        return "org.spongepowered.api.event.Event".equals(eventClass.getQualifiedName());
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "Parameter is not an instance of org.spongepowered.api.event.Event\n" +
        "Compiling and running this listener may result in a runtime exception";
    }

    @Override
    public List<PsiClass> getEventPossibilities(List<BuildDependency> dependencies) {
        BuildDependency spongeDependency = null;
        for (BuildDependency dependency : dependencies) {
            if (dependency.getArtifactId().equals("spongeapi")) {
                spongeDependency = dependency;
            }
        }
        if (spongeDependency == null) {
            return Collections.emptyList();
        }
        return super.getEventPossibilities(dependencies);
    }
}
