package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.util.Util;

import com.google.common.base.Strings;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftModule {

    private static Map<Module, MinecraftModule> map = new HashMap<>();

    private Module module;
    private BuildSystem buildSystem;
    private Map<AbstractModuleType<?>, AbstractModule> modules = new ConcurrentHashMap<>();

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
    public static synchronized MinecraftModule getInstance(@NotNull Module module) {
        if (map.containsKey(module)) {
            return map.get(module);
        } else {
            if (isModuleApplicable(module)) {
                MinecraftModule minecraftModule = map.put(module, createFromModule(module));
                Util.invokeLater(ProjectView.getInstance(module.getProject())::refresh);
                return minecraftModule;
            } else {
                String[] paths = ModuleManager.getInstance(module.getProject()).getModuleGroupPath(module);
                if (paths != null && paths.length > 0) {
                    Module parentModule = ModuleManager.getInstance(module.getProject()).findModuleByName(paths[0]);
                    if (parentModule != null) {
                        if (map.containsKey(parentModule)) {
                            MinecraftModule minecraftModule = map.get(parentModule);
                            // Save the parent module for this MinecraftModule so we don't have to do this check next time
                            map.put(module, minecraftModule);
                            return minecraftModule;
                        } else if (isModuleApplicable(parentModule)) {
                            MinecraftModule minecraftModule = map.put(parentModule, createFromModule(parentModule));
                            Util.invokeLater(ProjectView.getInstance(module.getProject())::refresh);
                            return minecraftModule;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static synchronized <T extends AbstractModule> T getInstance(@NotNull Module module, @NotNull AbstractModuleType<T> type) {
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

    public boolean isEventClassValidForModule(@NotNull PsiClass eventClass) {
        for (AbstractModule abstractModule : modules.values()) {
            if (abstractModule.isEventClassValid(eventClass, null)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEventClassValid(@NotNull PsiClass eventClass, @NotNull PsiMethod method) {
        for (AbstractModule abstractModule : modules.values()) {
            boolean good = abstractModule.getModuleType().getListenerAnnotations().stream()
                    .anyMatch(listenerAnnotation -> method.getModifierList().findAnnotation(listenerAnnotation) != null);

            if (good) {
                return abstractModule.isEventClassValid(eventClass, method);
            }
        }
        return false;
    }

    public String writeErrorMessageForEvent(@NotNull PsiClass eventClass, @NotNull PsiMethod method) {
        for (AbstractModule abstractModule : modules.values()) {
            boolean good = abstractModule.getModuleType().getListenerAnnotations().stream()
                    .anyMatch(listenerAnnotation -> method.getModifierList().findAnnotation(listenerAnnotation) != null);

            if (good) {
                return abstractModule.writeErrorMessageForEventParameter(eventClass, method);
            }
        }
        return null;
    }

    public void addModuleType(@NotNull String moduleTypeName) {
        AbstractModuleType<?> type = PlatformType.getByName(moduleTypeName);
        if (type != null && !modules.containsKey(type)) {
            modules.put(type, type.generateModule(module));
        }
        ProjectView.getInstance(module.getProject()).refresh();
    }

    public void removeModuleType(@NotNull String moduleTypeName) {
        AbstractModuleType<?> type = PlatformType.getByName(moduleTypeName);
        if (type != null && modules.containsKey(type)) {
            modules.remove(type);
        }
        ProjectView.getInstance(module.getProject()).refresh();
    }

    public boolean isEventGenAvailable() {
        return modules.keySet().stream().anyMatch(AbstractModuleType::isEventGenAvailable);
    }

    @Contract(value = "null -> false", pure = true)
    public boolean shouldShowPluginIcon(@Nullable PsiElement element) {
        return getModules().stream().filter(m -> m.shouldShowPluginIcon(element)).findAny().isPresent();
    }
}
