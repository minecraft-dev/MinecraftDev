package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.sponge.generation.SpongeGenerationData;

import com.intellij.openapi.module.Module;
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

import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

public class SpongeModule extends AbstractModule {

    SpongeModule(@NotNull Module module) {
        super(module);
        buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            if (!buildSystem.isImported()) {
                buildSystem.reImport(module);
            }
        }
    }

    @NotNull
    public Module getModule() {
        return module;
    }

    @Override
    public AbstractModuleType<SpongeModule> getModuleType() {
        return SpongeModuleType.getInstance();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.SPONGE;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.SPONGE_ICON;
    }

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        return "org.spongepowered.api.event.Event".equals(eventClass.getQualifiedName());
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "Parameter is not an instance of org.spongepowered.api.event.Event\n" +
        "Compiling and running this listener may result in a runtime exception";
    }

    @Override
    public List<PsiClass> getEventPossibilities(List<BuildDependency> dependencies) {
        BuildDependency spongeDependency = null;
        for (BuildDependency dependency : dependencies) {
            if (dependency.getArtifactId().equals("spongeapi")) {
                spongeDependency = dependency;
            }
        }
        if (spongeDependency == null) {
            return Collections.emptyList();
        }
        return super.getEventPossibilities(dependencies);
    }

    @Nullable
    @Override
    public PsiMethod generateEventListenerMethod(@NotNull PsiClass containingClass,
                                                 @NotNull PsiClass chosenClass,
                                                 @NotNull String chosenName,
                                                 @Nullable GenerationData data) {
        PsiMethod method = JavaPsiFacade.getElementFactory(project).createMethod(chosenName, PsiType.VOID);
        PsiParameterList parameterList = method.getParameterList();

        PsiParameter parameter = JavaPsiFacade.getElementFactory(project)
            .createParameter(
                "event",
                PsiClassType.getTypeByName(chosenClass.getQualifiedName(), project, GlobalSearchScope.allScope(project))
            );

        parameterList.add(parameter);
        PsiModifierList modifierList = method.getModifierList();

        PsiAnnotation listenerAnnotation = modifierList.addAnnotation("org.spongepowered.api.event.Listener");

        SpongeGenerationData generationData = (SpongeGenerationData) data;
        assert generationData != null;

        if (!generationData.isIgnoreCanceled()) {
            PsiAnnotation annotation = modifierList.addAnnotation("org.spongepowered.api.event.filter.IsCancelled");
            PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText("org.spongepowered.api.util.Tristate.UNDEFINED", annotation);

            annotation.setDeclaredAttributeValue("value", value);
        }

        if (!generationData.getEventOrder().equals("DEFAULT")) {
            PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText("org.spongepowered.api.event.Order." + generationData.getEventOrder(), listenerAnnotation);

            listenerAnnotation.setDeclaredAttributeValue("order", value);
        }

        return method;
    }
}
