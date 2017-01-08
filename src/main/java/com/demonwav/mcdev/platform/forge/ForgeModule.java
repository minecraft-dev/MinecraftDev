/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.inspection.IsCancelled;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.forge.util.ForgeConstants;
import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class ForgeModule extends AbstractModule {

    private VirtualFile mcmod;

    ForgeModule(@NotNull Module module) {
        super(module);
        this.buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            buildSystem.reImport(module).done(buildSystem -> mcmod = buildSystem.findFile(ForgeConstants.MCMOD_INFO, SourceType.RESOURCE));
        }
    }

    @Override
    public GradleBuildSystem getBuildSystem() {
        return (GradleBuildSystem) buildSystem;
    }


    @Override
    public ForgeModuleType getModuleType() {
        return ForgeModuleType.getInstance();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.FORGE;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.FORGE_ICON;
    }

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        if (method == null ) {
            return ForgeConstants.FML_EVENT.equals(eventClass.getQualifiedName()) ||
                ForgeConstants.EVENT.equals(eventClass.getQualifiedName());
        }

        PsiAnnotation annotation = method.getModifierList().findAnnotation(ForgeConstants.EVENT_HANDLER_ANNOTATION);
        if (annotation != null) {
            return ForgeConstants.FML_EVENT.equals(eventClass.getQualifiedName());
        }

        annotation = method.getModifierList().findAnnotation(ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION);
        if (annotation != null) {
            return ForgeConstants.EVENT.equals(eventClass.getQualifiedName());
        }

        // just default to true
        return true;
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        final PsiAnnotation annotation = method.getModifierList().findAnnotation(ForgeConstants.EVENT_HANDLER_ANNOTATION);

        if (annotation != null) {
            return "Parameter is not a subclass of net.minecraftforge.fml.common.event.FMLEvent\n" +
                    "Compiling and running this listener may result in a runtime exception";
        }

        return "Parameter is not a subclass of net.minecraftforge.fml.common.eventhandler.Event\n" +
                "Compiling and running this listener may result in a runtime exception";
    }

    @Override
    public boolean isStaticListenerSupported(@NotNull PsiClass eventClass, @NotNull PsiMethod method) {
        return true;
    }

    public VirtualFile getMcmod() {
        if (buildSystem == null) {
            buildSystem = BuildSystem.getInstance(module);
        }
        if (mcmod == null && buildSystem != null) {
            // try and find the file again if it's not already present
            // when this object was first created it may not have been ready
            mcmod = buildSystem.findFile(ForgeConstants.MCMOD_INFO, SourceType.RESOURCE);
        }
        return mcmod;
    }

    @Nullable
    @Override
    public PsiMethod generateEventListenerMethod(@NotNull PsiClass containingClass,
                                                 @NotNull PsiClass chosenClass,
                                                 @NotNull String chosenName,
                                                 @Nullable GenerationData data) {
        final boolean isFmlEvent = McPsiUtil.extendsOrImplementsClass(chosenClass, ForgeConstants.FML_EVENT);

        final PsiMethod method = JavaPsiFacade.getElementFactory(project).createMethod(chosenName, PsiType.VOID);
        final PsiParameterList parameterList = method.getParameterList();

        final PsiParameter parameter = JavaPsiFacade.getElementFactory(project)
            .createParameter(
                "event",
                PsiClassType.getTypeByName(chosenClass.getQualifiedName(), project, GlobalSearchScope.allScope(project))
            );

        parameterList.add(parameter);
        final PsiModifierList modifierList = method.getModifierList();

        if (isFmlEvent) {
            modifierList.addAnnotation(ForgeConstants.EVENT_HANDLER_ANNOTATION);
        } else {
            modifierList.addAnnotation(ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION);
        }

        return method;
    }

    @Override
    @Contract(value = "null -> false", pure = true)
    public boolean shouldShowPluginIcon(@Nullable PsiElement element) {
        if (!(element instanceof PsiIdentifier)) {
            return false;
        }

        if (!(element.getParent() instanceof PsiClass)) {
            return false;
        }

        final PsiClass psiClass = (PsiClass) element.getParent();

        final PsiModifierList modifierList = psiClass.getModifierList();
        return modifierList != null && modifierList.findAnnotation(ForgeConstants.MOD_ANNOTATION) != null;
    }

    @Nullable
    @Override
    public IsCancelled checkUselessCancelCheck(@NotNull PsiMethodCallExpression expression) {
        return null;
    }
}
