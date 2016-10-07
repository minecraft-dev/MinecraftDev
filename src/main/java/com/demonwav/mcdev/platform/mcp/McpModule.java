/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.mcp.srg.SrgManager;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class McpModule extends AbstractModule {
    public McpModule(@NotNull Module module) {
        super(module);
        this.buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            buildSystem.reImport(module);
            SrgManager.getInstance(this).recomputeIfNullAndGetSrgMap().rejected(System.out::println);
        }
    }

    @Override
    public GradleBuildSystem getBuildSystem() {
        return (GradleBuildSystem) buildSystem;
    }

    @Override
    public McpModuleType getModuleType() {
        return McpModuleType.getInstance();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.MCP;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        return super.isEventClassValid(eventClass, method);
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "";
    }
}
