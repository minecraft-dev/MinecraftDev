package com.demonwav.mcdev.platform.sponge.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Order(ExternalSystemConstants.UNORDERED)
public class SpongeProjectDataService extends AbstractProjectDataService<ModuleDependencyData, Module> {

    @NotNull
    @Override
    public Key<ModuleDependencyData> getTargetDataKey() {
        return ProjectKeys.MODULE_DEPENDENCY;
    }

    @Override
    public void importData(@NotNull Collection<DataNode<ModuleDependencyData>> toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
        if (projectData == null || !projectData.getOwner().equals(GradleConstants.SYSTEM_ID)) {
            return;
        }

        Set<Module> goodModules = toImport.stream()
                .filter(n -> n.getData().getExternalName().startsWith("SpongeAPI"))
                .map(n -> modelsProvider.findIdeModule(n.getData().getOwnerModule()))
                .distinct()
                .collect(Collectors.toSet());

        AbstractDataService.setupModules(goodModules, modelsProvider, SpongeModuleType.getInstance());
    }
}
