/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary;

import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.facet.MinecraftFacet;
import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.canary.generation.CanaryGenerationData;
import com.demonwav.mcdev.platform.canary.util.CanaryConstants;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CanaryModule<T extends AbstractModuleType> extends AbstractModule {

    private final T moduleType;
    private final PlatformType type;
    private VirtualFile canaryInf;

    public CanaryModule(@NotNull MinecraftFacet facet, @NotNull T type) {
        super(facet);
        this.moduleType = type;
        this.type = type.getPlatformType();
        setup();
    }

    private void setup() {
        canaryInf = facet.findFile(CanaryConstants.CANARY_INF, SourceType.RESOURCE);
    }

    @NotNull
    public VirtualFile getCanaryInf() {
        if (canaryInf == null) {
            // try and find the file again if it's not already present
            // when this object was first created it may not have been ready
            setup();
        }
        return canaryInf;
    }

    @Override
    public T getModuleType() {
        return moduleType;
    }

    @Override
    public PlatformType getType() {
        return type;
    }

    @Override
    public boolean isEventClassValid(@NotNull PsiClass eventClass, @Nullable PsiMethod method) {
        return CanaryConstants.HOOK_CLASS.equals(eventClass.getQualifiedName());
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "Parameter is not a subclass of " + CanaryConstants.HOOK_CLASS + "\n" +
                "Compiling and running this listener may result in a runtime exception";
    }
    @Nullable
    @Override
    public PsiMethod generateEventListenerMethod(@NotNull PsiClass containingClass,
            @NotNull PsiClass chosenClass,
            @NotNull String chosenName,
            @Nullable GenerationData data) {
        CanaryGenerationData canaryData = (CanaryGenerationData) data;
        assert canaryData != null;

        final PsiMethod method = generateCanaryStyleEventListenerMethod(
                containingClass,
                chosenClass,
                chosenName,
                project,
                CanaryConstants.HOOK_HANDLER_ANNOTATION,
                canaryData.isIgnoreCanceled()
        );

        if (!canaryData.getPriority().equals("NORMAL")) {
            final PsiModifierList list = method.getModifierList();
            final PsiAnnotation annotation = list.findAnnotation(CanaryConstants.HOOK_HANDLER_ANNOTATION);
            if (annotation == null) {
                return method;
            }

            final PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(project)
                    .createExpressionFromText(CanaryConstants.PRIORITY_CLASS + "." + canaryData.getPriority(), annotation);

            annotation.setDeclaredAttributeValue("priority", value);
        }

        return method;
    }

    public static PsiMethod generateCanaryStyleEventListenerMethod(@NotNull PsiClass containingClass,
                                                                   @NotNull PsiClass chosenClass,
                                                                   @NotNull String chosenName,
                                                                   @NotNull Project project,
                                                                   @NotNull String annotationName,
                                                                   boolean setIgnoreCancelled) {
        final PsiMethod newMethod = JavaPsiFacade.getElementFactory(project).createMethod(chosenName, PsiType.VOID);

        final PsiParameterList list = newMethod.getParameterList();
        final PsiParameter parameter = JavaPsiFacade.getElementFactory(project)
                .createParameter(
                        "event",
                        PsiClassType.getTypeByName(chosenClass.getQualifiedName(), project, GlobalSearchScope.allScope(project))
                );
        list.add(parameter);

        final PsiModifierList modifierList = newMethod.getModifierList();
        final PsiAnnotation annotation = modifierList.addAnnotation(annotationName);

        if (setIgnoreCancelled) {
            final PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(project).createExpressionFromText("true", annotation);
            annotation.setDeclaredAttributeValue("ignoreCanceled", value);
        }

        return newMethod;
    }

    @Override
    public void dispose() {
        super.dispose();

        canaryInf = null;
    }
}
