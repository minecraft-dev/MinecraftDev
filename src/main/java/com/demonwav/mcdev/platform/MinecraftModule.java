package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.buildsystem.BuildSystem;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinecraftModule {

    private static Map<Module, MinecraftModule> map = new HashMap<>();

    private Module module;
    private BuildSystem buildSystem;
    private List<AbstractModule> modules = new ArrayList<>();
    private List<AbstractModuleType> types = new ArrayList<>();

    public static MinecraftModule generate(List<AbstractModuleType> types, Module module) {
        MinecraftModule minecraftModule = new MinecraftModule();
        types.forEach(type -> {
            minecraftModule.register(type, module);
        });
        minecraftModule.module = module;
        minecraftModule.buildSystem = BuildSystem.getInstance(module);
        return minecraftModule;
    }

    private static MinecraftModule createFromModule(@NotNull Module module) {
        List<AbstractModuleType> types = new ArrayList<>();
        String option = module.getOptionValue(MinecraftModuleType.OPTION);
        if (Strings.isNullOrEmpty(option)) {
            return null;
        }

        String[] typeStrings = option.split(",");
        for (String typeString : typeStrings) {
            types.add((AbstractModuleType) ModuleTypeManager.getInstance().findByID(typeString));
        }

        return generate(types, module);
    }

    @Nullable
    public static MinecraftModule getInstance(@NotNull Module module) {
        if (map.containsKey(module)) {
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

    private static boolean isModuleApplicable(@NotNull Module module) {
        ModuleType type = ModuleUtil.getModuleType(module);
        if (type == MinecraftModuleType.getInstance()) {
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

    private void register(AbstractModuleType type, Module module) {
        modules.add(type.generateModule(module));
        types.add(type);
    }

    public List<AbstractModule> getModules() {
        return ImmutableList.copyOf(modules);
    }

    public List<AbstractModuleType> getTypes() {
        return ImmutableList.copyOf(types);
    }

    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        return modules.stream().anyMatch(abstractModule -> abstractModule.isEventClassValid(eventClass, method));
    }

    public String writeErrorMessageForEvent(PsiClass eventClass, PsiModifierList modifierList) {
        for (AbstractModule abstractModule : modules) {
            boolean good = abstractModule.getModuleType().getListenerAnnotations().stream()
                    .anyMatch(listenerAnnotation -> modifierList.findAnnotation(listenerAnnotation) != null);

            if (good) {
                return abstractModule.writeErrorMessageForEventParameter(eventClass);
            }
        }
        return null;
    }
}
