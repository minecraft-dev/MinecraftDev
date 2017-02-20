/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.swing.Icon;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MinecraftModule {

    private static final Map<Module, MinecraftModule> map = new HashMap<>();
    private static final Set<MinecraftModule> pendingModules = Sets.newHashSet();
    private static final Set<Consumer<MinecraftModule>> readyWaiters = Sets.newConcurrentHashSet();

    private final Module module;
    private final BuildSystem buildSystem;
    private final Map<AbstractModuleType<?>, AbstractModule> modules = new ConcurrentHashMap<>();

    private MinecraftModule(Module module, BuildSystem buildSystem) {
        this.module = module;
        this.buildSystem = buildSystem;
    }

    @NotNull
    private static MinecraftModule generate(@NotNull List<AbstractModuleType<?>> types, @NotNull Module module) {
        final MinecraftModule minecraftModule = new MinecraftModule(module, BuildSystem.getInstance(module));
        pendingModules.add(minecraftModule);
        if (minecraftModule.buildSystem != null) {
            minecraftModule.buildSystem.reImport(module).done(buildSystem -> {
                types.forEach(minecraftModule::register);
                // Startup actions
                pendingModules.remove(minecraftModule);
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

        final MinecraftModule minecraftModule = generate(types, module);
        map.put(module, minecraftModule);
        Util.invokeLater(ProjectView.getInstance(module.getProject())::refresh);
        return minecraftModule;
    }

    @Nullable
    public static synchronized MinecraftModule getInstance(@NotNull Module module) {
        if (map.containsKey(module)) {
            return map.get(module);
        }

        if (isModuleApplicable(module)) {
            return createFromModule(module);
        }

        final String[] paths = ModuleManager.getInstance(module.getProject()).getModuleGroupPath(module);
        if (paths == null || paths.length == 0) {
            return null;
        }

        final Module parentModule;
        try (final AccessToken ignored = ApplicationManager.getApplication().acquireReadActionLock()) {
            parentModule = ModuleManager.getInstance(module.getProject()).findModuleByName(paths[paths.length - 1]);
        }

        if (parentModule == null) {
            return null;
        }

        if (map.containsKey(parentModule)) {
            final MinecraftModule minecraftModule = map.get(parentModule);
            // Save the parent module for this MinecraftModule so we don't have to do this check next time
            map.put(module, minecraftModule);
            return minecraftModule;
        } else if (isModuleApplicable(parentModule)) {
            return createFromModule(parentModule);
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

    @Contract(pure = true)
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

    @Contract(pure = true)
    public Module getIdeaModule() {
        return module;
    }

    @Contract(pure = true)
    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    private void register(@NotNull AbstractModuleType<?> type) {
        type.performCreationSettingSetup(module.getProject());
        modules.put(type, type.generateModule(module));
    }

    @NotNull
    @Contract(pure = true)
    public Collection<AbstractModule> getModules() {
        return modules.values();
    }

    @NotNull
    @Contract(pure = true)
    public Collection<AbstractModuleType<?>> getTypes() {
        return modules.keySet();
    }

    @Contract(value = "null -> false", pure = true)
    public boolean isOfType(@Nullable AbstractModuleType<?> type) {
        return modules.containsKey(type);
    }

    @Nullable
    @Contract(value = "null -> null", pure = true)
    public <T extends AbstractModule> T getModuleOfType(@Nullable AbstractModuleType<T> type) {
        //noinspection unchecked
        return (T) modules.get(type);
    }

    @Contract(value = "null -> false", pure = true)
    public boolean isEventClassValidForModule(@Nullable PsiClass eventClass) {
        if (eventClass == null) {
            return false;
        }

        for (AbstractModule abstractModule : modules.values()) {
            if (abstractModule.isEventClassValid(eventClass, null)) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
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
        modules.keySet().removeIf(next -> !platformTypes.contains(next.getPlatformType()));
        for (PlatformType type : types) {
            if (!modules.keySet().contains(type.getType())) {
                modules.put(type.getType(), type.getType().generateModule(module));
            }
        }
        ProjectView.getInstance(module.getProject()).refresh();
    }

    @Contract(pure = true)
    public boolean isEventGenAvailable() {
        return modules.keySet().stream().anyMatch(AbstractModuleType::isEventGenAvailable);
    }

    @Contract(value = "null -> false", pure = true)
    public boolean shouldShowPluginIcon(@Nullable PsiElement element) {
        return getModules().stream().anyMatch(m -> m.shouldShowPluginIcon(element));
    }

    @Nullable
    @Contract(pure = true)
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
            .filter(Objects::nonNull)
            .findFirst();
    }

    public static void doWhenReady(@NotNull Consumer<MinecraftModule> consumer) {
        readyWaiters.add(consumer);
    }

    private static void doReadyActions(@NotNull MinecraftModule module) {
        if (!module.getIdeaModule().isDisposed() && !module.getIdeaModule().getProject().isDisposed()) {
            for (Consumer<MinecraftModule> readyWaiter : readyWaiters) {
                readyWaiter.accept(module);
            }
        }

        // We don't want to hold on to the actions after we've finished setting up
        if (pendingModules.isEmpty()) {
            readyWaiters.clear();
        }
    }
}
