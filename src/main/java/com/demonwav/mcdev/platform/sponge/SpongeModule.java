/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.facet.MinecraftFacet;
import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.inspection.IsCancelled;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.sponge.generation.SpongeGenerationData;
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants;
import com.demonwav.mcdev.util.McPsiClass;
import com.demonwav.mcdev.util.McPsiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.compiled.ClsMethodImpl;
import com.intellij.psi.search.GlobalSearchScope;
import javax.swing.Icon;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpongeModule extends AbstractModule {

    SpongeModule(@NotNull MinecraftFacet facet) {
        super(facet);
    }

    @NotNull
    public Module getModule() {
        return module;
    }

    @Override
    public AbstractModuleType<SpongeModule> getModuleType() {
        return SpongeModuleType.INSTANCE;
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
    public boolean isEventClassValid(@NotNull PsiClass eventClass, @Nullable PsiMethod method) {
        return "org.spongepowered.api.event.Event".equals(eventClass.getQualifiedName());
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "Parameter is not an instance of org.spongepowered.api.event.Event\n" +
        "Compiling and running this listener may result in a runtime exception";
    }

    @Nullable
    @Override
    public PsiMethod generateEventListenerMethod(@NotNull PsiClass containingClass,
                                                 @NotNull PsiClass chosenClass,
                                                 @NotNull String chosenName,
                                                 @Nullable GenerationData data) {
        final PsiMethod method = JavaPsiFacade.getElementFactory(project).createMethod(chosenName, PsiType.VOID);
        final PsiParameterList parameterList = method.getParameterList();

        final PsiParameter parameter = JavaPsiFacade.getElementFactory(project)
            .createParameter(
                "event",
                PsiClassType.getTypeByName(chosenClass.getQualifiedName(), project, GlobalSearchScope.allScope(project))
            );

        parameterList.add(parameter);
        final PsiModifierList modifierList = method.getModifierList();

        final PsiAnnotation listenerAnnotation = modifierList.addAnnotation("org.spongepowered.api.event.Listener");

        final SpongeGenerationData generationData = (SpongeGenerationData) data;
        assert generationData != null;

        if (!generationData.isIgnoreCanceled()) {
            final PsiAnnotation annotation = modifierList.addAnnotation("org.spongepowered.api.event.filter.IsCancelled");
            final PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText("org.spongepowered.api.util.Tristate.UNDEFINED", annotation);

            annotation.setDeclaredAttributeValue("value", value);
        }

        if (!generationData.getEventOrder().equals("DEFAULT")) {
            final PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText("org.spongepowered.api.event.Order." + generationData.getEventOrder(), listenerAnnotation);

            listenerAnnotation.setDeclaredAttributeValue("order", value);
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
        return modifierList != null && modifierList.findAnnotation(SpongeConstants.PLUGIN_ANNOTATION) != null;
    }

    @Nullable
    @Override
    public IsCancelled checkUselessCancelCheck(@NotNull PsiMethodCallExpression expression) {
        final PsiMethod method = McPsiUtil.findContainingMethod(expression);
        if (method == null) {
            return null;
        }

        // Make sure this is an event listener method
        final PsiAnnotation listenerAnnotation = method.getModifierList().findAnnotation(SpongeConstants.LISTENER_ANNOTATION);
        if (listenerAnnotation == null) {
            return null;
        }

        boolean isCancelled = false;
        final PsiAnnotation annotation = method.getModifierList().findAnnotation(SpongeConstants.IS_CANCELLED_ANNOTATION);
        if (annotation != null) {
            final PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
            if (value == null) {
                return null;
            }

            final String text = value.getText();

            if (text.indexOf('.') == -1) {
                return null;
            }

            final String sub = text.substring(text.lastIndexOf('.') + 1);
            switch (sub) {
                case "TRUE":
                    isCancelled = true;
                    break;
                case "FALSE":
                    isCancelled = false;
                    break;
                case "UNDEFINED":
                default:
                    return null;
            }
        }

        final PsiReferenceExpression methodExpression = expression.getMethodExpression();
        final PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
        final PsiElement resolve = methodExpression.resolve();

        if (qualifierExpression == null) {
            return null;
        }
        if (resolve == null) {
            return null;
        }

        if (standardSkip(method, qualifierExpression)) {
            return null;
        }

        final PsiElement content = resolve.getContext();
        if (!(content instanceof PsiClass)) {
            return null;
        }

        final PsiClass psiClass = (PsiClass) content;
        if (!McPsiClass.extendsOrImplements(psiClass, SpongeConstants.CANCELLABLE)) {
            return null;
        }

        if (!(resolve instanceof ClsMethodImpl)) {
            return null;
        }

        if (!((ClsMethodImpl) resolve).getName().equals(SpongeConstants.EVENT_ISCANCELLED_METHOD_NAME)) {
            return null;
        }

        final IsCancelled.IsCancelledBuilder isCancelledBuilder =
            IsCancelled.builder()
                       .setErrorString(
                           "Cancellable.isCancelled() check is useless in a method not annotated with @IsCancelled(Tristate.UNDEFINED)"
                       );

        if (isCancelled) {
            isCancelledBuilder.setFix(descriptor ->
                expression.replace(JavaPsiFacade.getElementFactory(project).createExpressionFromText("true", expression)));
        } else {
            isCancelledBuilder.setFix(descriptor ->
                expression.replace(JavaPsiFacade.getElementFactory(project).createExpressionFromText("false", expression)));
        }

        return isCancelledBuilder.build();
    }
}
