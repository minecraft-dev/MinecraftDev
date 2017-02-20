/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;
import com.google.common.collect.Sets;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.AbstractDependencyData;
import com.intellij.openapi.externalSystem.model.project.DependencyData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.util.GradleConstants;

@Order(ExternalSystemConstants.UNORDERED)
public class SpongeDataService extends AbstractProjectDataService<ModuleData, Module> {

    private static final String spongeMatcher = SpongeModuleType.getInstance().getGroupId() + ":" + SpongeModuleType.getInstance().getArtifactId();

    @NotNull
    @Override
    public Key<ModuleData> getTargetDataKey() {
        return ProjectKeys.MODULE;
    }

    @Override
    public void importData(@NotNull Collection<DataNode<ModuleData>> toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
        if (projectData == null || !projectData.getOwner().equals(GradleConstants.SYSTEM_ID)) {
            return;
        }

        final Set<Module> allModules = Sets.newHashSet();

        Set<Module> goodModules = toImport.stream()
            .flatMap(n -> n.getChildren().stream())
            .flatMap(n -> {
                if (n.getData() instanceof GradleSourceSetData) {
                    return n.getChildren().stream()
                        .filter(n1 -> n1.getData() instanceof DependencyData)
                        .filter(n1 -> ((DependencyData) n1.getData()).getOwnerModule().getExternalName().contains("main"));
                } else {
                    return Stream.of(n);
                }
            })
            .filter(n -> n.getData() instanceof AbstractDependencyData)
            .peek(n -> allModules.add(modelsProvider.findIdeModule(((DependencyData) n.getData()).getOwnerModule())))
            .filter(n -> ((AbstractDependencyData) n.getData()).getExternalName().toLowerCase().matches("(^" + spongeMatcher + "|.*sponge(common|api)).*"))
            .map(n -> modelsProvider.findIdeModule(((DependencyData) n.getData()).getOwnerModule()))
            .collect(Collectors.toSet());

        goodModules.addAll(toImport.stream()
            .filter(n -> n.getData().getId().toLowerCase().contains("spongeapi"))
            .map(n -> modelsProvider.findIdeModule(n.getData().getInternalName()))
            .collect(Collectors.toSet()));

        AbstractDataService.setupModules(goodModules, allModules, modelsProvider, SpongeModuleType.getInstance());
    }
}
