package com.demonwav.mcdev.platform.mixin.util;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;
import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class MixinUtils {
    private MixinUtils() {}

    /**
     * Given any PsiElement, determine if it resides in a {@link com.demonwav.mcdev.platform.mixin.MixinModule MixinModule}.
     *
     * @param element The element to check.
     * @return True if this element resides in a {@link com.demonwav.mcdev.platform.mixin.MixinModule MixinModule}.
     */
    @Contract(value = "null -> false", pure = true)
    public static boolean isMixinModule(@Nullable PsiElement element) {
        return element != null && isMixinModule(ModuleUtilCore.findModuleForPsiElement(element));
    }

    /**
     * Given any Module, determine if it is a {@link com.demonwav.mcdev.platform.mixin.MixinModule MixinModule}.
     *
     * @param module The module to check.
     * @return True if this module is a {@link com.demonwav.mcdev.platform.mixin.MixinModule MixinModule}.
     */
    @Contract(value = "null -> false", pure = true)
    public static boolean isMixinModule(@Nullable Module module) {
        return module != null && isMixinModule(MinecraftModule.getInstance(module));
    }

    /**
     * Given any MinecraftModule instance, determine if it contains a {@link com.demonwav.mcdev.platform.mixin.MixinModule MixinModule}.
     *
     * @param instance The instance to check.
     * @return True if this instance contains a {@link com.demonwav.mcdev.platform.mixin.MixinModule MixinModule}.
     */
    @Contract(value = "null -> false", pure = true)
    public static boolean isMixinModule(@Nullable MinecraftModule instance) {
        return instance != null && instance.isOfType(MixinModuleType.getInstance());
    }

    /**
     * Given a PsiElement, return the PsiClass it is in, if and only if this PsiClass it a Mixin class. If this class is not a Mixin class,
     * or the given element is null, return null.
     *
     * @param element The element to check.
     * @return The PsiClass the element is in if and only if that class is a Mixin class, otherwise null.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiClass getContainingMixinClass(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        final PsiClass classOfElement = McPsiUtil.getClassOfElement(element);
        if (classOfElement == null) {
            return null;
        }

        if (getMixinAnnotationFromClass(classOfElement) == null) {
            return null;
        }

        return classOfElement;
    }

    /**
     * Get the Mixin PsiAnnotation for the Mixin class which contains the given PsiElement. Returns null if the provided element is null or
     * the element is not in a Mixin class.
     *
     * @param element The PsiElement to check.
     * @return The Mixin PsiAnnotation for the Mixin class which contains the given PsiElement.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiAnnotation getMixinAnnotationOfContainingClass(@Nullable PsiElement element) {
        return getMixinAnnotationFromClass(McPsiUtil.getClassOfElement(element));
    }

    /**
     * Get the Mixin PsiAnnotation for the provided Mixin PsiClass. Returns null if the provided class is null or the class is not a
     * Mixin class.
     *
     * @param psiClass The PsiClass to check.
     * @return The Mixin PsiAnnotation for the provided Mixin PsiClass.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiAnnotation getMixinAnnotationFromClass(@Nullable PsiClass psiClass) {
        return McPsiUtil.getAnnotation(psiClass, MixinConstants.Annotations.MIXIN);
    }

    /**
     * For Mixin classes which define a {@code target} value in the Mixin annotation, return the first value in that list.
     * Returns null if there is no {@code target} value in the Mixin annotation, or the first class in the {@code target} annotation cannot
     * be resolved.
     *
     * @param psiClass The PsiClass to check.
     * @return The first class defined in the {@code target} value in the Mixin annotation of the given PsiClass.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiClass getFirstTargetOfMixinClass(@Nullable PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }

        final List<PsiClass> targets = getAllTargetsOfMultiTargetMixinClass(psiClass);
        if (targets.isEmpty()) {
            return null;
        }

        return targets.get(0);
    }

    /**
     * For Mixin classes which define a {@code target} value in the Mixin annotation, return all classes defined in that list.
     * Returns an empty list if there is no {@code target} value in the Mixin annotation, or no classes in the {@code target} annotation
     * can be resolved.
     *
     * @param psiClass The PsiClass to check.
     * @return A list of classes defined in the {@code target} value in the Mixin annotation of the given PsiClass.
     */
    @NotNull
    @Contract(pure = true)
    public static List<PsiClass> getAllTargetsOfMultiTargetMixinClass(@Nullable PsiClass psiClass) {
        if (psiClass == null) {
            return Collections.emptyList();
        }

        // TODO
        return Collections.emptyList();
    }

    /**
     * Return the single {@code value} PsiClass target of the provided Mixin class. Returns null if the provided class is null,
     * there is no value set for the Mixin annotation, or the value cannot be resolved to a class.
     *
     * @param psiClass The PsiClass to check.
     * @return The single {@code value} PsiClass target of the provided Mixin class.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiClass getValueTargetOfMixinClass(@Nullable PsiClass psiClass) {
        final PsiAnnotationMemberValue value = getMemberValueTargetOfMixinClass(psiClass);
        return McPsiUtil.resolveGenericClass(value);
    }

    /**
     * Return the PsiAnnotationMemberValue of the single {@code value} field of the Mixin annotation on the provided class. This is useful
     * if you need the reference to the PsiAnnotationMemberValue, but want to resolve the class later, as resolving is an expensive process.
     * Returns null if the provided class is null, is not a Mixin class, or if the attribute value does not exist.
     *
     * You can easily resolve the class later using {@link McPsiUtil#resolveGenericClass(PsiAnnotationMemberValue)}.
     *
     * @param psiClass The PsiClass to check.
     * @return The single {@code value} PsiAnnotationMemberValue target of the provided Mixin class.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiAnnotationMemberValue getMemberValueTargetOfMixinClass(@Nullable PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }

        final PsiAnnotation annotation = getMixinAnnotationFromClass(psiClass);
        if (annotation == null) {
            return null;
        }

        return annotation.findDeclaredAttributeValue("value");
    }

    /**
     * Get a list of every PsiClass target defined in the Mixin annotation of the given class. Returns an empty list if this is not a Mixin
     * class or if there are no resolvable targets defined in the Mixin annotation.
     *
     * @param psiClass The PsiClass to check.
     * @return A list of every PsiClass target defined in the Mixin annotation of the given class.
     */
    @NotNull
    @Contract(pure = true)
    public static List<PsiClass> getAllTargetsOfMixinClass(@Nullable PsiClass psiClass) {
        final List<PsiClass> list = getAllTargetsOfMultiTargetMixinClass(psiClass);
        final PsiClass value = getValueTargetOfMixinClass(psiClass);
        if (value != null) {
            list.add(value);
        }

        return list;
    }
}
