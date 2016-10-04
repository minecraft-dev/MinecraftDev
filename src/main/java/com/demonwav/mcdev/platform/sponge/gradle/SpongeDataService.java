package com.demonwav.mcdev.platform.sponge.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.DependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
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
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Order(ExternalSystemConstants.UNORDERED)
public class SpongeDataService extends AbstractProjectDataService<ModuleData, Module> {

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

        Set<Module> goodModules = toImport.stream()
            .flatMap(n -> n.getChildren().stream())
            .flatMap(n -> {
                Object d = n.getData();
                if (d instanceof GradleSourceSetData) {
                    return n.getChildren().stream()
                        .filter(n1 -> n1.getData() instanceof DependencyData)
                        .filter(n1 -> ((DependencyData) n1.getData()).getOwnerModule().getExternalName().contains("main"));
                } else {
                    return Stream.of(n);
                }
            })
            .filter(n -> n.getData() instanceof DependencyData)
            .filter(n -> {
                final Object d = n.getData();
                if (d instanceof LibraryDependencyData) {
                    return ((LibraryDependencyData) d).getExternalName()
                        .startsWith(SpongeModuleType.getInstance().getGroupId() + ":" + SpongeModuleType.getInstance().getArtifactId());
                } else {
                    return ((ModuleDependencyData) d).getExternalName().contains("SpongeCommon") ||
                        ((ModuleDependencyData) d).getExternalName().contains("SpongeAPI");
                }
            })
            .map(n -> ((DependencyData) n.getData()).getOwnerModule())
            .map(modelsProvider::findIdeModule)
            .collect(Collectors.toSet());

        AbstractDataService.setupModules(goodModules, modelsProvider, SpongeModuleType.getInstance());
    }
}
