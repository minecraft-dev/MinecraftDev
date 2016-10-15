/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.canary.util.CanaryConstants;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

public class CanaryModule<T extends CanaryModuleType> extends AbstractModule {

    private final T moduleType;
    private final PlatformType type;
    private VirtualFile canaryInf;

    public CanaryModule(@NotNull Module module, @NotNull T type) {
        super(module);
        this.moduleType = type;
        this.type = type.getPlatformType();
        buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            buildSystem.reImport(module).done(b -> setup());
        }
    }

    private void setup() {
        canaryInf = buildSystem.findFile("Canary.inf", SourceType.RESOURCE);
    }

    public VirtualFile getCanaryInf() {
        if (buildSystem == null) {
            buildSystem = BuildSystem.getInstance(module);
        }
        if (canaryInf == null && buildSystem != null) {
            // try and find the file again if it's not already present
            // when this object was first created it may not have been ready
            canaryInf = buildSystem.findFile("Canary.inf", SourceType.RESOURCE);
        }
        return canaryInf;
    }

    @Override
    public T getModuleType() {
        return moduleType;
    }

    @Override
    public PlatformType getType() {
        return type;
    }

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        return CanaryConstants.HOOK_CLASS.equals(eventClass.getQualifiedName());
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "Parameter is not a subclass of " + CanaryConstants.HOOK_CLASS + "\n" +
                "Compiling and running this listener may result in a runtime exception";
    }

}
