/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.inspection.IsCancelled;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.generation.BukkitGenerationData;
import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants;
import com.demonwav.mcdev.util.McPsiUtil;

import com.google.common.base.Objects;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.compiled.ClsMethodImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import javax.swing.Icon;

@SuppressWarnings("unused")
public class BukkitModule<T extends BukkitModuleType> extends AbstractModule {

    private VirtualFile pluginYml;
    private PlatformType type;
    private T moduleType;

    BukkitModule(@NotNull Module module, @NotNull T type) {
        super(module);
        this.moduleType = type;
        this.type = type.getPlatformType();
        buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            buildSystem.reImport(module).done(buildSystem -> setup());
        }
    }

    private void setup() {
        pluginYml = buildSystem.findFile("plugin.yml", SourceType.RESOURCE);
    }

    public VirtualFile getPluginYml() {
        if (buildSystem == null) {
            buildSystem = BuildSystem.getInstance(module);
        }
        if (pluginYml == null && buildSystem != null) {
            // try and find the file again if it's not already present
            // when this object was first created it may not have been ready
            pluginYml = buildSystem.findFile("plugin.yml", SourceType.RESOURCE);
        }
        return pluginYml;
    }

    @Override
    public Icon getIcon() {
        return type.getType().getIcon();
    }

    @Override
    public T getModuleType() {
        return moduleType;
    }

    private void setModuleType(@NotNull T moduleType) {
        this.moduleType = moduleType;
    }

    @Override
    public PlatformType getType() {
        return type;
    }

    private void setType(@NotNull PlatformType type) {
        this.type = type;
    }

    @Override
    public boolean isEventClassValid(@NotNull PsiClass eventClass, @Nullable PsiMethod method) {
        return BukkitConstants.EVENT_CLASS.equals(eventClass.getQualifiedName());
    }

    @Override
    public boolean isStaticListenerSupported(@NotNull PsiClass eventClass, @NotNull PsiMethod method) {
        return true;
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "Parameter is not a subclass of org.bukkit.event.Event\n" +
                "Compiling and running this listener may result in a runtime exception";
    }

    @Override
    public void doPreEventGenerate(@NotNull PsiClass psiClass, @Nullable GenerationData data) {
        if (!McPsiUtil.extendsOrImplementsClass(psiClass, BukkitConstants.LISTENER_CLASS)) {
            McPsiUtil.addImplements(psiClass, BukkitConstants.LISTENER_CLASS, project);
        }
    }

    @Nullable
    @Override
    public PsiMethod generateEventListenerMethod(@NotNull PsiClass containingClass,
                                                 @NotNull PsiClass chosenClass,
                                                 @NotNull String chosenName,
                                                 @Nullable GenerationData data) {
        BukkitGenerationData bukkitData = (BukkitGenerationData) data;
        assert  bukkitData != null;

        final PsiMethod method = generateBukkitStyleEventListenerMethod(
            containingClass,
            chosenClass,
            chosenName,
            project,
            BukkitConstants.HANDLER_ANNOTATION,
            bukkitData.isIgnoreCanceled()
        );

        if (!bukkitData.getEventPriority().equals("NORMAL")) {
            final PsiModifierList list = method.getModifierList();
            final PsiAnnotation annotation = list.findAnnotation(BukkitConstants.HANDLER_ANNOTATION);
            if (annotation == null) {
                return method;
            }

            final PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(project)
                .createExpressionFromText(BukkitConstants.EVENT_PRIORITY_CLASS + "." + bukkitData.getEventPriority(), annotation);

            annotation.setDeclaredAttributeValue("priority", value);
        }

        return method;
    }

    public static PsiMethod generateBukkitStyleEventListenerMethod(@NotNull PsiClass containingClass,
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
            annotation.setDeclaredAttributeValue("ignoreCancelled", value);
        }

        return newMethod;
    }

    @Nullable
    @Override
    public IsCancelled checkUselessCancelCheck(@NotNull PsiMethodCallExpression expression) {
        final PsiMethod method = McPsiUtil.findParent(expression, PsiMethod.class);
        if (method == null) {
            return null;
        }

        final PsiAnnotation annotation = method.getModifierList().findAnnotation(BukkitConstants.HANDLER_ANNOTATION);
        if (annotation == null) {
            return null;
        }

        // We are in an event method
        final PsiAnnotationMemberValue annotationMemberValue = annotation.findAttributeValue("ignoreCancelled");
        if (!(annotationMemberValue instanceof PsiLiteralExpression)) {
            return null;
        }

        final PsiLiteralExpression value = (PsiLiteralExpression) annotationMemberValue;
        if (!(value.getValue() instanceof Boolean)) {
            return null;
        }

        final boolean ignoreCancelled = (Boolean) value.getValue();

        // If we aren't ignoring cancelled then any check for event being cancelled is valid
        if (!ignoreCancelled) {
            return null;
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

        final PsiElement context = resolve.getContext();
        if (!(context instanceof PsiClass)) {
            return null;
        }

        final PsiClass psiClass = (PsiClass) context;
        if (!McPsiUtil.extendsOrImplementsClass(psiClass, BukkitConstants.CANCELLABLE_CLASS)) {
            return null;
        }

        if (!(resolve instanceof ClsMethodImpl)) {
            return null;
        }

        if (!((ClsMethodImpl) resolve).getName().equals(BukkitConstants.EVENT_ISCANCELLED_METHOD_NAME)) {
            return null;
        }

        return IsCancelled.builder()
                          .setErrorString("Cancellable.isCancelled() check is useless in a method annotated with ignoreCancelled=true.")
                          .setFix(descriptor -> expression
                              .replace(JavaPsiFacade.getElementFactory(project).createExpressionFromText("false", expression)))
                          .build();
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

        final Project project = element.getProject();

        final PsiClass psiClass = (PsiClass) element.getParent();

        final PsiClass javaPluginClass = JavaPsiFacade.getInstance(project)
            .findClass(BukkitConstants.JAVA_PLUGIN, GlobalSearchScope.allScope(project));

        return javaPluginClass != null &&
            Arrays.stream(psiClass.getExtendsListTypes())
                .anyMatch(c -> c.equals(PsiTypesUtil.getClassType(javaPluginClass)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final BukkitModule<?> that = (BukkitModule<?>) o;
        return Objects.equal(pluginYml, that.pluginYml) &&
            type == that.type &&
            Objects.equal(moduleType, that.moduleType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pluginYml, type, moduleType);
    }
}
