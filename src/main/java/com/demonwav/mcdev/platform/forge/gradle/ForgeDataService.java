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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.util.Collection;

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

        ForgeModuleType  type = ForgeModuleType.getInstance();
        ApplicationManager.getApplication().runReadAction(() -> {
            final Module module = modelsProvider.getModules()[0];
            if (!checkModule(module, modelsProvider)) {
                return;
            }

            for (Module m : modelsProvider.getModules()) {
                if (m.equals(module)) {
                    m.setOption("type", type.getId());
                    java.util.Optional.ofNullable(BuildSystem.getInstance(m)).ifPresent(md -> md.reImport(module, type.getPlatformType()));
                } else {
                    if (Strings.nullToEmpty(m.getOptionValue("type")).equals(type.getId())) {
                        m.setOption("type", JavaModuleType.getModuleType().getId());
                    }
                }
            }
        });
    }

    @Contract("null, _ -> false")
    private boolean checkModule(Module module, IdeModifiableModelsProvider provider) {
        if (module != null) {
            VirtualFile[] roots = provider.getModifiableRootModel(module).getContentRoots();
            if (roots.length == 0) {
                // last ditch effort
                roots = ModuleRootManager.getInstance(module).getContentRoots();
                if (roots.length == 0) {
                    return false;
                }
            }
            VirtualFile file = roots[0];
            file = file.findFileByRelativePath("build.gradle");

            if (file != null) {
                GroovyFile groovyFile = (GroovyFile) PsiManager.getInstance(module.getProject()).findFile(file);
                if (groovyFile != null) {
                    if (groovyFile.getText().contains("apply plugin: 'net.minecraftforge.gradle.forge'")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
