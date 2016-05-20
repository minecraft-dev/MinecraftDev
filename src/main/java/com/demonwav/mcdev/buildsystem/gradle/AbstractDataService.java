package com.demonwav.mcdev.buildsystem.gradle;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.google.common.base.Strings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Order(ExternalSystemConstants.UNORDERED)
public abstract class AbstractDataService extends AbstractProjectDataService<LibraryDependencyData, Module> {

    @NotNull
    private final MinecraftModuleType type;

    public AbstractDataService(@NotNull final MinecraftModuleType type) {
        this.type = type;
    }

    @NotNull
    @Override
    public Key<LibraryDependencyData> getTargetDataKey() {
        return ProjectKeys.LIBRARY_DEPENDENCY;
    }

    @Override
    public void importData(@NotNull Collection<DataNode<LibraryDependencyData>> toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
        if (projectData == null || !projectData.getOwner().equals(GradleConstants.SYSTEM_ID)) {
            return;
        }

        // Set module type accordingly
        Set<Module> goodModules = toImport.stream()
                .filter(n -> n.getData().getExternalName().startsWith(type.getGroupId() + ":" + type.getArtifactId()))
                .map(n -> modelsProvider.findIdeModule(n.getData().getOwnerModule()))
                .distinct()
                .collect(Collectors.toSet());

        // So the way the Gradle plugin sets it up is with 3 modules. There's the parent module, which the Gradle
        // dependencies don't apply to, then submodules under it, normally main and test, which the Gradle dependencies
        // do apply to. We're interested (when setting the module type) in the parent, which is what we do here. The
        // first module should be the parent, but we check to make sure anyways
        ApplicationManager.getApplication().runReadAction(() -> {
            final Module module = modelsProvider.getModules()[0];
            if (module != null) {
                if (modelsProvider.getModules().length == 1) {
                    // Okay so all that up there is only one case. The other case is when it's just a single module
                    if (goodModules.contains(module)) {
                        module.setOption("type", type.getId());
                        Optional.ofNullable(BuildSystem.getInstance(module)).ifPresent(m -> m.reImport(module, type.getPlatformType()));
                    } else {
                        if (Strings.nullToEmpty(module.getOptionValue("type")).equals(type.getId())) {
                            module.setOption("type", JavaModuleType.getModuleType().getId());
                        }
                    }
                } else {
                    // This is a group of modules
                    goodModules.forEach(m -> {
                        Project currentProject = m.getProject();
                        String[] paths = ModuleManager.getInstance(currentProject).getModuleGroupPath(m);
                        if (paths != null && paths.length > 1) {
                            // The last element will be this module, the second to last is the parent
                            String parentName = paths[paths.length - 2];
                            Module parentModule = ModuleManager.getInstance(project).findModuleByName(parentName);

                            if (Objects.equals(parentModule, module)) {
                                VirtualFile root = ModuleRootManager.getInstance(module).getContentRoots()[0];
                                VirtualFile gradle = root.findChild("build.gradle");

                                if (gradle != null) {
                                    module.setOption("type", type.getId());
                                    Optional.ofNullable(BuildSystem.getInstance(module)).ifPresent(thisModule -> thisModule.reImport(module, type.getPlatformType()));
                                    return;
                                }
                            }
                        }
                        if (Strings.nullToEmpty(module.getOptionValue("type")).equals(type.getId())) {
                            module.setOption("type", JavaModuleType.getModuleType().getId());
                        }
                    });
                }
            }
        });
    }
}
