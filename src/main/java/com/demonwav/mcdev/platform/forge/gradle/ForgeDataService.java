package com.demonwav.mcdev.platform.forge.gradle;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.google.common.base.Strings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class ForgeDataService extends AbstractDataService {
    public ForgeDataService() {
        super(ForgeModuleType.getInstance());
    }

    @Override
    public void importData(@NotNull Collection<DataNode<LibraryDependencyData>> toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
        if (projectData == null || !projectData.getOwner().equals(GradleConstants.SYSTEM_ID)) {
            return;
        }

        Set<Module> goodModules = toImport.stream()
                .map(n -> {
                    Module module = modelsProvider.findIdeModule(n.getData().getOwnerModule());
                    if (module != null) {
                        VirtualFile file = modelsProvider.getModifiableRootModel(module).getContentRoots()[0];
                        file = file.findFileByRelativePath("build.gradle");

                        if (file != null) {
                            GroovyFile groovyFile = (GroovyFile) PsiManager.getInstance(project).findFile(file);
                            if (groovyFile != null) {
                                if (groovyFile.getText().contains("apply plugin: 'net.minecraftforge.gradle.forge'")) {
                                    return module;
                                }
                            }
                        }
                    }
                    return null;
                })
                .filter(m -> m != null)
                .distinct()
                .collect(Collectors.toSet());

        ForgeModuleType  type = ForgeModuleType.getInstance();
        ApplicationManager.getApplication().runReadAction(() -> {
            final Module module = modelsProvider.getModules()[0];
            if (module != null) {
                if (modelsProvider.getModules().length == 1) {
                    // Okay so all that up there is only one case. The other case is when it's just a single module
                    if (goodModules.contains(module)) {
                        module.setOption("type", type.getId());
                        java.util.Optional.ofNullable(BuildSystem.getInstance(module)).ifPresent(m -> m.reImport(module, type.getPlatformType()));
                    } else {
                        if (Strings.nullToEmpty(module.getOptionValue("type")).equals(type.getId())) {
                            module.setOption("type", JavaModuleType.getModuleType().getId());
                        }
                    }
                } else {
                    // This is a group of modules
                    if (goodModules.stream().anyMatch(m -> {
                        String[] paths = ModuleManager.getInstance(project).getModuleGroupPath(m);
                        if (paths != null && paths.length > 0) {
                            if (Arrays.stream(paths).anyMatch(module.getName()::equals)) {
                                return true;
                            }
                        }
                        return false;
                    })) {
                        module.setOption("type", type.getId());
                        java.util.Optional.ofNullable(BuildSystem.getInstance(module)).ifPresent(m -> m.reImport(module, type.getPlatformType()));
                    } else {
                        if (Strings.nullToEmpty(module.getOptionValue("type")).equals(type.getId())) {
                            module.setOption("type", JavaModuleType.getModuleType().getId());
                        }
                    }
                }
            }
        });
    }
}
