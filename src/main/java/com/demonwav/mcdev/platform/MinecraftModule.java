/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;
import com.demonwav.mcdev.util.Util;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.swing.Icon;

public class MinecraftModule {

    private static Map<Module, MinecraftModule> map = new HashMap<>();
    private static Set<Consumer<MinecraftModule>> readyWaiters = Sets.newConcurrentHashSet();

    private Module module;
    private BuildSystem buildSystem;
    private Map<AbstractModuleType<?>, AbstractModule> modules = new ConcurrentHashMap<>();

    private static MinecraftModule generate(@NotNull List<AbstractModuleType<?>> types, @NotNull Module module) {
        final MinecraftModule minecraftModule = new MinecraftModule();
        minecraftModule.module = module;
        minecraftModule.buildSystem = BuildSystem.getInstance(module);
        if (minecraftModule.buildSystem != null) {
            minecraftModule.buildSystem.reImport(module).done(buildSystem -> {
                types.forEach(minecraftModule::register);
                doReadyActions(minecraftModule);
            });
        }
        return minecraftModule;
    }

    @Nullable
    private static MinecraftModule createFromModule(@NotNull Module module) {
        final List<AbstractModuleType<?>> types = new ArrayList<>();
        final String option = module.getOptionValue(MinecraftModuleType.OPTION);
        if (Strings.isNullOrEmpty(option)) {
            return null;
        }

        final String[] typeStrings = option.split(",");
        for (String typeString : typeStrings) {
            final AbstractModuleType<?> type = PlatformType.getByName(typeString);
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
                final MinecraftModule minecraftModule = map.put(module, createFromModule(module));
                Util.invokeLater(ProjectView.getInstance(module.getProject())::refresh);
                return minecraftModule;
            } else {
                final String[] paths = ModuleManager.getInstance(module.getProject()).getModuleGroupPath(module);
                if (paths != null && paths.length > 0) {
                    final Module parentModule;
                    try (final AccessToken ignored = ApplicationManager.getApplication().acquireReadActionLock()) {
                        parentModule = ModuleManager.getInstance(module.getProject()).findModuleByName(paths[paths.length - 1]);
                    }
                    if (parentModule != null) {
                        if (map.containsKey(parentModule)) {
                            final MinecraftModule minecraftModule = map.get(parentModule);
                            // Save the parent module for this MinecraftModule so we don't have to do this check next time
                            map.put(module, minecraftModule);
                            return minecraftModule;
                        } else if (isModuleApplicable(parentModule)) {
                            final MinecraftModule minecraftModule = map.put(parentModule, createFromModule(parentModule));
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
        final MinecraftModule minecraftModule = getInstance(module);
        if (minecraftModule != null) {
            return minecraftModule.getModuleOfType(type);
        }
        return null;
    }

    private static boolean isModuleApplicable(@NotNull Module module) {
        final ModuleType type = ModuleUtil.getModuleType(module);
        if (type == JavaModuleType.getModuleType()) {
            final String option = module.getOptionValue(MinecraftModuleType.OPTION);
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
    public <T extends AbstractModule> T getModuleOfType(@Nullable AbstractModuleType<T> type) {
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

    public boolean isStaticListenerSupported(@NotNull PsiClass eventClass, @NotNull PsiMethod method) {
        for (AbstractModule abstractModule : modules.values()) {
            boolean good = abstractModule.getModuleType().getListenerAnnotations().stream()
                .anyMatch(listenerAnnotation -> method.getModifierList().findAnnotation(listenerAnnotation) != null);

            if (good) {
                return abstractModule.isStaticListenerSupported(eventClass, method);
            }
        }
        return false;
    }

    public void addModuleType(@NotNull String moduleTypeName) {
        final AbstractModuleType<?> type = PlatformType.getByName(moduleTypeName);
        if (type != null && !modules.containsKey(type)) {
            modules.put(type, type.generateModule(module));
        }
        ProjectView.getInstance(module.getProject()).refresh();
    }

    public void removeModuleType(@NotNull String moduleTypeName) {
        final AbstractModuleType<?> type = PlatformType.getByName(moduleTypeName);
        if (type != null && modules.containsKey(type)) {
            modules.remove(type);
        }
        ProjectView.getInstance(module.getProject()).refresh();
    }

    /**
     * Synchronize this module with the types given
     *
     * @param types Types to synchronize off of
     */
    public void updateModules(PlatformType[] types) {
        final List<PlatformType> platformTypes = Arrays.asList(types);
        for (Iterator<AbstractModuleType<?>> iter = modules.keySet().iterator(); iter.hasNext();) {
            final AbstractModuleType<?> next = iter.next();
            if (!platformTypes.contains(next.getPlatformType())) {
                iter.remove();
            }
        }
        for (PlatformType type : types) {
            if (!modules.keySet().contains(type.getType())) {
                modules.put(type.getType(), type.getType().generateModule(module));
            }
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

    @Nullable
    public Icon getIcon() {
        if (modules.keySet().stream().filter(AbstractModuleType::hasIcon).count() == 1) {
            return modules.values().iterator().next().getIcon();
        } else if (
            modules.keySet().stream().filter(AbstractModuleType::hasIcon).count() == 2 &&
            modules.containsKey(SpongeModuleType.getInstance()) &&
            modules.containsKey(ForgeModuleType.getInstance())
        ) {
            return PlatformAssets.SPONGE_FORGE_ICON;
        } else {
            return PlatformAssets.MINECRAFT_ICON;
        }
    }

    @NotNull
    public static Optional<VirtualFile> searchAllModulesForFile(@NotNull String path, @NotNull SourceType type) {
        return map.values().stream().distinct()
            .filter(m -> m != null && m.getBuildSystem() != null)
            .map(m -> m.getBuildSystem().findFile(path, type))
            .filter(f -> f != null)
            .findFirst();
    }

    public static void doWhenReady(@NotNull Consumer<MinecraftModule> consumer) {
        readyWaiters.add(consumer);
    }

    public static void cleanReadyActions() {
        readyWaiters.clear();
    }

    private static void doReadyActions(@NotNull MinecraftModule module) {
        if (!module.getIdeaModule().getProject().isDisposed()) {
            for (Consumer<MinecraftModule> readyWaiter : readyWaiters) {
                readyWaiter.accept(module);
            }
        }
    }
}
