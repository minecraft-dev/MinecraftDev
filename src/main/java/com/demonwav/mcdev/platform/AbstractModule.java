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

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.inspection.IsCancelled;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public abstract class AbstractModule {

    protected final Project project;
    protected final Module module;
    protected BuildSystem buildSystem;

    public AbstractModule(@NotNull Module module) {
        this.module = module;
        this.project = module.getProject();
    }

    @Contract(pure = true)
    public Module getModule() {
        return module;
    }

    @Contract(pure = true)
    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    @Contract(pure = true)
    public abstract AbstractModuleType<?> getModuleType();

    @Contract(pure = true)
    public abstract PlatformType getType();

    @Contract(pure = true)
    public Icon getIcon() {
        return getModuleType().getIcon();
    }

    /**
     * By default, this method is provided in the case that a specific platform has no
     * listener handling whatsoever, or simply accepts event listeners with random
     * classes. This is rather open ended. Primarily this should (platform dependent)
     * evaluate to the type (or multiple types) to determine whether the event listener
     * is not going to throw an error at runtime.
     *
     * @param eventClass The PsiClass of the event listener argument
     * @param method The method of the event listener
     * @return True if the class is valid or ignored. Returning false may highlight the
     *     method as an error and prevent compiling.
     */
    public boolean isEventClassValid(@NotNull PsiClass eventClass, @Nullable PsiMethod method) {
        return false;
    }

    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "Parameter does not extend the proper Event Class!";
    }

    public void doPreEventGenerate(@NotNull PsiClass psiClass, @Nullable GenerationData data) {
    }

    @Nullable
    public PsiMethod generateEventListenerMethod(@NotNull PsiClass containingClass,
                                                 @NotNull PsiClass chosenClass,
                                                 @NotNull String chosenName,
                                                 @Nullable GenerationData data) {
        return null;
    }

    @Contract(value = "null -> false", pure = true)
    public boolean shouldShowPluginIcon(@Nullable PsiElement element) {
        return false;
    }

    @Nullable
    @Contract(pure = true)
    public IsCancelled checkUselessCancelCheck(@NotNull PsiMethodCallExpression expression) {
        return null;
    }

    public boolean isStaticListenerSupported(@NotNull PsiClass eventClass, @NotNull PsiMethod method) {
        return false;
    }
}
