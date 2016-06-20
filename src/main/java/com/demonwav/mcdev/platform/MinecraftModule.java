package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;

import com.google.common.base.Strings;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MinecraftModule {

    private static Map<Module, MinecraftModule> map = new HashMap<>();

    private Module module;
    private BuildSystem buildSystem;
    private Map<AbstractModuleType<?>, AbstractModule> modules = new HashMap<>();

    private static MinecraftModule generate(@NotNull List<AbstractModuleType<?>> types, @NotNull Module module) {
        MinecraftModule minecraftModule = new MinecraftModule();
        minecraftModule.module = module;
        minecraftModule.buildSystem = BuildSystem.getInstance(module);
        if (minecraftModule.buildSystem != null) {
            minecraftModule.buildSystem.reImport(module).done(buildSystem -> types.forEach(minecraftModule::register));
        }
        return minecraftModule;
    }

    @Nullable
    private static MinecraftModule createFromModule(@NotNull Module module) {
        List<AbstractModuleType<?>> types = new ArrayList<>();
        String option = module.getOptionValue(MinecraftModuleType.OPTION);
        if (Strings.isNullOrEmpty(option)) {
            return null;
        }

        String[] typeStrings = option.split(",");
        for (String typeString : typeStrings) {
            AbstractModuleType<?> type = PlatformType.getByName(typeString);
            if (type != null) {
                types.add(type);
            }
        }

        return generate(types, module);
    }

    @Nullable
    public static MinecraftModule getInstance(@NotNull Module module) {
        if (map.containsKey(module)) {
            MinecraftModule minecraftModule = map.get(module);
            minecraftModule.checkModule();
            return minecraftModule;
        } else {
            if (isModuleApplicable(module)) {
                MinecraftModule minecraftModule = map.put(module, createFromModule(module));
                ProjectView.getInstance(module.getProject()).refresh();
                return minecraftModule;
            } else {
                String[] paths = ModuleManager.getInstance(module.getProject()).getModuleGroupPath(module);
                if (paths != null && paths.length > 0) {
                    Module parentModule = ModuleManager.getInstance(module.getProject()).findModuleByName(paths[0]);
                    if (parentModule != null) {
                        if (map.containsKey(parentModule)) {
                            MinecraftModule minecraftModule = map.get(parentModule);
                            map.put(module, minecraftModule);
                            minecraftModule.checkModule();
                            return minecraftModule;
                        } else if (isModuleApplicable(parentModule)) {
                            MinecraftModule minecraftModule = map.put(parentModule, createFromModule(parentModule));
                            ProjectView.getInstance(module.getProject()).refresh();
                            return minecraftModule;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static <T extends AbstractModule> T getInstance(@NotNull Module module, @NotNull AbstractModuleType<T> type) {
        MinecraftModule minecraftModule = getInstance(module);
        if (minecraftModule != null) {
            return minecraftModule.getModuleOfType(type);
        }
        return null;
    }

    private static boolean isModuleApplicable(@NotNull Module module) {
        ModuleType type = ModuleUtil.getModuleType(module);
        if (type == JavaModuleType.getModuleType()) {
            String option = module.getOptionValue(MinecraftModuleType.OPTION);
            if (!Strings.isNullOrEmpty(option)) {
                return true;
            }
        }
        return false;
    }

    public void checkModule() {
        if (module.getProject().isDisposed()) {
            return;
        }

        if (buildSystem == null) {
            return;
        }

        if (!buildSystem.isFinishImport()) {
            if (!buildSystem.isImported()) {
                buildSystem.reImport(module);
            }
            return;
        }

        if (buildSystem.getDependencies() == null) {
            //  The importer doesn't work sometimes? It seems to be an IntelliJ issue?
            return;
        }

        String moduleTypesString = module.getOptionValue(MinecraftModuleType.OPTION);
        if (moduleTypesString == null) {
            return;
        }

        List<String> moduleTypes = Arrays.asList(moduleTypesString.split(","));
        List<String> modifiableModuleTypes = new ArrayList<>(moduleTypes);

        List<String> toBeRemoved = modules.keySet().stream().map(AbstractModuleType::getId)
                .filter(t -> !moduleTypes.contains(t)).collect(Collectors.toList());

        modules.forEach((type, module) -> {
            if (!buildSystem.getDependencies().stream().anyMatch(buildDependency ->
                    buildDependency.getArtifactId().equals(type.getArtifactId()) && buildDependency.getGroupId().equals(type.getGroupId())
            ) && !(type instanceof ForgeModuleType)) {
                if (!toBeRemoved.contains(type.getPlatformType().getName())) {
                    toBeRemoved.add(type.getPlatformType().getName());
                }
            }
        });

        if (toBeRemoved.size() > 0) {
            for (String s : toBeRemoved) {
                AbstractModuleType<?> type = PlatformType.getByName(s);
                if (type != null) {
                    modules.remove(type);
                    modifiableModuleTypes.remove(s);
                }
            }

            // Write the changes to the module settings
            module.setOption(MinecraftModuleType.OPTION, modifiableModuleTypes.stream().collect(Collectors.joining(",")));

            ApplicationManager.getApplication().invokeLater(() -> ProjectView.getInstance(module.getProject()).refresh());
        }
    }

    public Module getIdeaModule() {
        return module;
    }

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    private void register(@NotNull AbstractModuleType<?> type) {
        type.performCreationSettingSetup(module.getProject());
        modules.put(type, type.generateModule(module));
    }

    @NotNull
    public Collection<AbstractModule> getModules() {
        return modules.values();
    }

    @NotNull
    public Collection<AbstractModuleType<?>> getTypes() {
        return modules.keySet();
    }

    public boolean isOfType(@Nullable AbstractModuleType<?> type) {
        return modules.containsKey(type);
    }

    @Nullable
    private <T extends AbstractModule> T getModuleOfType(@Nullable AbstractModuleType<T> type) {
        //noinspection unchecked
        return (T) modules.get(type);
    }

    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        for (AbstractModule abstractModule : modules.values()) {
            boolean good = abstractModule.getModuleType().getListenerAnnotations().stream()
                    .anyMatch(listenerAnnotation -> method.getModifierList().findAnnotation(listenerAnnotation) != null);

            if (good) {
                return abstractModule.isEventClassValid(eventClass, method);
            }
        }
        return false;
    }

    public String writeErrorMessageForEvent(PsiClass eventClass, PsiMethod method) {
        for (AbstractModule abstractModule : modules.values()) {
            boolean good = abstractModule.getModuleType().getListenerAnnotations().stream()
                    .anyMatch(listenerAnnotation -> method.getModifierList().findAnnotation(listenerAnnotation) != null);

            if (good) {
                return abstractModule.writeErrorMessageForEventParameter(eventClass, method);
            }
        }
        return null;
    }

    public void addModuleType(String moduleTypeName) {
        AbstractModuleType<?> type = PlatformType.getByName(moduleTypeName);
        if (type != null && !modules.containsKey(type)) {
            modules.put(type, type.generateModule(module));
        }
        ProjectView.getInstance(module.getProject()).refresh();
    }

    public void removeModuleType(String moduleTypeName) {
        AbstractModuleType<?> type = PlatformType.getByName(moduleTypeName);
        if (type != null && modules.containsKey(type)) {
            modules.remove(type);
        }
        ProjectView.getInstance(module.getProject()).refresh();
    }
}
