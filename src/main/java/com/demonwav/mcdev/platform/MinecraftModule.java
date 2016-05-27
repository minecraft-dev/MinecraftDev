package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.buildsystem.BuildSystem;

import com.google.common.base.Strings;
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
        types.forEach(minecraftModule::register);
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
            return map.get(module);
        } else {
            if (isModuleApplicable(module)) {
                return map.put(module, createFromModule(module));
            } else {
                String[] paths = ModuleManager.getInstance(module.getProject()).getModuleGroupPath(module);
                if (paths != null && paths.length > 0) {
                    Module parentModule = ModuleManager.getInstance(module.getProject()).findModuleByName(paths[0]);
                    if (parentModule != null) {
                        if (map.containsKey(parentModule)) {
                            MinecraftModule minecraftModule = map.get(parentModule);
                            minecraftModule.checkModule();
                            map.put(module, minecraftModule);
                            return map.get(module);
                        } else if (isModuleApplicable(parentModule)) {
                            return map.put(parentModule, createFromModule(parentModule));
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

    private void checkModule() {
        String moduleTypesString = module.getOptionValue(MinecraftModuleType.OPTION);
        if (moduleTypesString == null) {
            return;
        }

        List<String> moduleTypes = Arrays.asList(moduleTypesString.split(","));


        List<String> toBeAdded = moduleTypes.stream().filter(t -> {
            AbstractModuleType<?> moduleType = PlatformType.getByName(t);
            return !modules.containsKey(moduleType);
        }).collect(Collectors.toList());

        List<String> toBeRemoved = modules.keySet().stream().map(AbstractModuleType::getId)
                .filter(t -> !moduleTypes.contains(t)).collect(Collectors.toList());

        if (toBeAdded.size() > 0) {
            for (String moduleType : toBeAdded) {
                AbstractModuleType<?> abstractModuleType= PlatformType.getByName(moduleType);
                if (abstractModuleType != null) {
                    register(abstractModuleType);
                }
            }
        }

        if (toBeRemoved.size() > 0) {
            for (String s : toBeRemoved) {
                AbstractModuleType<?> type = PlatformType.getByName(s);
                if (type != null) {
                    modules.remove(type);
                }
            }
        }
    }

    public Module getIdeaModule() {
        return module;
    }

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    private void register(@NotNull AbstractModuleType<?> type) {
        type.performCreationSettingSetup(module);
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
    public <T extends AbstractModule> T getModuleOfType(@Nullable AbstractModuleType<T> type) {
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
}
