package com.demonwav.mcdev.buildsystem.gradle;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.util.*;
import java.util.stream.Collectors;

@Order(ExternalSystemConstants.UNORDERED)
public abstract class AbstractDataService extends AbstractProjectDataService<LibraryDependencyData, Module> {

    @NotNull
    private final AbstractModuleType<?> type;

    public AbstractDataService(@NotNull final AbstractModuleType type) {
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

        Set<Module> goodModules = toImport.stream()
                .filter(n -> n.getData().getExternalName().startsWith(type.getGroupId() + ":" + type.getArtifactId()))
                .map(n -> modelsProvider.findIdeModule(n.getData().getOwnerModule()))
                .distinct()
                .collect(Collectors.toSet());

        setupModules(goodModules, modelsProvider, type);
    }

    /**
     * We have two checks for Sponge, so we hold the first one here until the second one finishes. This is so hacky it hurts.
     */
    private static Set<Module> firstGoodModules;

    public static void setupModules(@NotNull Set<Module> goodModules,
                                    @NotNull IdeModifiableModelsProvider modelsProvider,
                                    @NotNull AbstractModuleType<?> type) {

        if (type == SpongeModuleType.getInstance()) {
            if (firstGoodModules == null) {
                firstGoodModules = goodModules;
                return;
            }

            goodModules.addAll(firstGoodModules);
        }

        // So the way the Gradle plugin sets it up is with 3 modules. There's the parent module, which the Gradle
        // dependencies don't apply to, then submodules under it, normally main and test, which the Gradle dependencies
        // do apply to. We're interested (when setting the module type) in the parent, which is what we do here. The
        // first module should be the parent, but we check to make sure anyways
        ApplicationManager.getApplication().runReadAction(() -> {
            Set<Module> checkedModules = new HashSet<>();
            Set<Module> badModules = new HashSet<>();
            checkedModules.addAll(goodModules);

            goodModules.stream().forEach(m -> {
                String[] path = modelsProvider.getModifiableModuleModel().getModuleGroupPath(m);
                if (path == null) {
                    // Always reset back to JavaModule
                    m.setOption("type", JavaModuleType.getModuleType().getId());
                    checkedModules.add(m);
                    MinecraftModuleType.addOption(m, type.getId());
                    Optional.ofNullable(BuildSystem.getInstance(m)).ifPresent(thisModule -> thisModule.reImport(m));
                    MinecraftModule.getInstance(m);
                } else {
                    String parentName = path[0];
                    Module parentModule = modelsProvider.getModifiableModuleModel().findModuleByName(parentName);
                    if (parentModule != null) {
                        // Always reset back to JavaModule
                        parentModule.setOption("type", JavaModuleType.getModuleType().getId());
                        badModules.add(m);
                        checkedModules.add(parentModule);
                        MinecraftModuleType.addOption(parentModule, type.getId());
                        Optional.ofNullable(BuildSystem.getInstance(parentModule)).ifPresent(thisModule -> thisModule.reImport(parentModule));
                        MinecraftModule.getInstance(parentModule);
                    }
                }
            });

            // Reset all other modules back to JavaModule && remove the type
            for (Module module : modelsProvider.getModules()) {
                if (!checkedModules.contains(module) || badModules.contains(module)) {
                    if (Strings.nullToEmpty(module.getOptionValue("type")).equals(type.getId())) {
                        module.setOption("type", JavaModuleType.getModuleType().getId());
                    }
                    MinecraftModuleType.removeOption(module, type.getId());
                }
            }
        });

        if (firstGoodModules != null) {
            firstGoodModules = null;
        }
    }

    protected void checkModule(@NotNull IdeModifiableModelsProvider modelsProvider,
                                  @NotNull AbstractModuleType<?> type,
                                  @NotNull String text) {
        ApplicationManager.getApplication().runReadAction(() -> {
            final Module[] modules = modelsProvider.getModules();
            List<Module> forgeModules = new ArrayList<>();
            for (Module module : modules) {
                if (!checkModuleText(module, modelsProvider, text)) {
                    // Make sure this isn't marked as a forge module
                    MinecraftModuleType.removeOption(module, type.getId());
                    continue;
                }

                forgeModules.add(module);
            }

            for (Module testModule : modelsProvider.getModules()) {
                if (forgeModules.contains(testModule)) {
                    testModule.setOption("type", JavaModuleType.getModuleType().getId());
                    MinecraftModuleType.addOption(testModule, type.getId());
                    Optional.ofNullable(BuildSystem.getInstance(testModule)).ifPresent(md -> md.reImport(testModule));
                    MinecraftModule.getInstance(testModule);
                } else {
                    if (Strings.nullToEmpty(testModule.getOptionValue("type")).equals(type.getId())) {
                        testModule.setOption("type", JavaModuleType.getModuleType().getId());
                    }
                }
            }
        });
    }

    @Contract("null, _, _ -> false")
    protected boolean checkModuleText(Module module, IdeModifiableModelsProvider provider, String text) {
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
                    if (groovyFile.getText().contains(text)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
