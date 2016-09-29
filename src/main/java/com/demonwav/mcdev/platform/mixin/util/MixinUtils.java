package com.demonwav.mcdev.platform.mixin.util;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;
import com.demonwav.mcdev.util.McMethodUtil;
import com.demonwav.mcdev.util.McPsiUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.AnonymousElementProvider;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiImmediateClassType;
import com.intellij.psi.impl.source.tree.ElementType;
import com.intellij.psi.impl.source.tree.java.PsiClassObjectAccessExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
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

        if (getMixinAnnotation(classOfElement) == null) {
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
        return getMixinAnnotation(McPsiUtil.getClassOfElement(element));
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
    public static PsiAnnotation getMixinAnnotation(@Nullable PsiClass psiClass) {
        return McPsiUtil.getAnnotation(psiClass, MixinConstants.Annotations.MIXIN);
    }

    /**
     * For Mixin classes which define a {@code targets} attribute in the Mixin annotation, return the first value in that list.
     * Returns null if there is no {@code target} attribute in the Mixin annotation, or the first class in the {@code target} annotation
     * cannot be resolved.
     *
     * @param psiClass The PsiClass to check.
     * @return The first target class pair defined in the {@code targets} attribute in the Mixin annotation of the given PsiClass.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static Pair<PsiElement, PsiClass> getFirstTargetOfTarget(@Nullable PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }

        final Map<PsiElement, PsiClass> targets = getAllMixedClassesOfTarget(psiClass);
        if (targets.isEmpty()) {
            return null;
        }

        Map.Entry<PsiElement, PsiClass> next = targets.entrySet().iterator().next();
        return Pair.create(next.getKey(), next.getValue());
    }

    /**
     * For Mixin classes which define a {@code targets} attribute in the Mixin annotation, return all classes defined in that list.
     * Returns an empty list if there is no {@code target} attribute in the Mixin annotation, or no classes in the {@code target} annotation
     * can be resolved.
     *
     * @param psiClass The PsiClass to check.
     * @return A map of targets defined in the {@code targets} attribute in the Mixin annotation to the relevant PsiClasses they point to.
     */
    @NotNull
    @Contract(pure = true)
    public static Map<PsiElement, PsiClass> getAllMixedClassesOfTarget(@Nullable PsiClass psiClass) {
        return resolveGenericClass(getMixinAnnotationTarget(psiClass));
    }

    /**
     * For Mixin classes which define a {@code value} attribute in the Mixin annotation, return the first value in that list.
     * Returns null if there is no {@code value} attribute in the Mixin annotation, or the first class in the {@code value} annotation
     * cannot be resolved.
     *
     * @param psiClass The PsiClass to check.
     * @return The first target class pair defined in the {@code value} attribute in the Mixin annotation of the given PsiClass.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static Pair<PsiElement, PsiClass> getFirstTargetOfValue(@Nullable PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }

        final Map<PsiElement, PsiClass> classes = getAllMixedClassesOfValue(psiClass);
        if (classes.isEmpty()) {
            return null;
        }
        Map.Entry<PsiElement, PsiClass> next = classes.entrySet().iterator().next();
        return Pair.create(next.getKey(), next.getValue());
    }

    /**
     * For Mixin classes which define a {@code value} attribute in the Mixin annotation, return all classes defined in that list.
     * Returns an empty list if there is no {@code value} attribute in the Mixin annotation, or no classes in the {@code target} annotation
     * can be resolved.
     *
     * @param psiClass The PsiClass to check.
     * @return A map of targets defined in the {@code value} attribute in the Mixin annotation to the relevant PsiClasses they point to.
     */
    @NotNull
    @Contract(pure = true)
    public static Map<PsiElement, PsiClass> getAllMixedClassesOfValue(@Nullable PsiClass psiClass) {
        return resolveGenericClass(getMixinAnnotationValue(psiClass));
    }

    /**
     * Return the PsiAnnotationMemberValue of the {@code targets} attribute of the Mixin annotation on the provided class. This is useful
     * if you need the reference to the PsiAnnotationMemberValue, but want to resolve the classes later, as resolving is an expensive
     * process. Returns null if the provided class is null, is not a Mixin class, or if the attribute target does not exist.
     *
     * You can easily resolve the classes later using {@link #resolveGenericClass(PsiAnnotationMemberValue)}.
     *
     * @param psiClass The PsiClass to check.
     * @return The {@code target} PsiAnnotationMemberValue targets of the provided Mixin class.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiAnnotationMemberValue getMixinAnnotationTarget(@Nullable PsiClass psiClass) {
        return getMixinAnnotationAttribute(psiClass, "targets");
    }

    /**
     * Return the PsiAnnotationMemberValue of the {@code value} attribute of the Mixin annotation on the provided class. This is useful
     * if you need the reference to the PsiAnnotationMemberValue, but want to resolve the classes later, as resolving is an expensive
     * process. Returns null if the provided class is null, is not a Mixin class, or if the attribute value does not exist.
     *
     * You can easily resolve the classes later using {@link #resolveGenericClass(PsiAnnotationMemberValue)}.
     *
     * @param psiClass The PsiClass to check.
     * @return The {@code value} PsiAnnotationMemberValue targets of the provided Mixin class.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiAnnotationMemberValue getMixinAnnotationValue(@Nullable PsiClass psiClass) {
        return getMixinAnnotationAttribute(psiClass, "value");
    }

    /**
     * Return the PsiAnnotationMemberValue of the given attribute of the Mixin annotation on the provided class. Returns null if the
     * provided class is null, is not a Mixin class, or if the attribute value does not exist.
     *
     * @param psiClass The PsiClass to check.
     * @param attribute The attribute to look for.
     * @return The {@code value} PsiAnnotationMemberValue targets of the provided Mixin class.
     */
    @Nullable
    @Contract("null, _ -> null")
    public static PsiAnnotationMemberValue getMixinAnnotationAttribute(@Nullable PsiClass psiClass, @NotNull String attribute) {
        if (psiClass == null) {
            return null;
        }

        final PsiAnnotation annotation = getMixinAnnotation(psiClass);
        if (annotation == null) {
            return null;
        }

        return annotation.findDeclaredAttributeValue(attribute);
    }

    /**
     * Get a list of every PsiClass target defined in the Mixin annotation of the given class. Returns an empty list if this is not a Mixin
     * class or if there are no resolvable targets defined in the Mixin annotation.
     *
     * @param psiClass The PsiClass to check.
     * @return A map of every attribute target defined in the given class and the PsiClass it relates to.
     */
    @NotNull
    @Contract(pure = true)
    public static Map<PsiElement, PsiClass> getAllMixedClasses(@Nullable PsiClass psiClass) {
        final Map<PsiElement, PsiClass> map = getAllMixedClassesOfTarget(psiClass);
        map.putAll(getAllMixedClassesOfValue(psiClass));
        return map;
    }

    /**
     * Given a Mixin target class string (the string given to the {@code targets} Mixin annotation attribute), find, if possible,
     * the corresponding class.
     *
     * @param s The String to check.
     * @return The corresponding class for the given String, or null if not found.
     */
    @Nullable
    @Contract(value = "null, _ -> null", pure = true)
    public static PsiClass getClassFromMixinTargetString(@Nullable String s, @NotNull Project project) {
        if (s == null) {
            return null;
        }

        final String replaced = s.replaceAll("/", ".");
        String text = replaced;
        if (text.contains("$")) {
            text = text.substring(0, text.indexOf('$'));
        }

        final PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(text, GlobalSearchScope.allScope(project));
        if (!replaced.contains("$")) {
            return psiClass;
        }

        if (psiClass == null) {
            return null;
        }

        // Handle anonymous classes
        final String[] classes = replaced.substring(replaced.indexOf('$')).split("\\$");
        List<Integer> indexes = Lists.newArrayList();
        for (String cls : classes) {
            if (cls.isEmpty()) {
                continue;
            }

            try {
                indexes.add(Integer.parseInt(cls) - 1);
            } catch (Exception e) {
                return psiClass;
            }
        }

        PsiElement current = psiClass;
        for (int index : indexes) {
            PsiElement[] anonymousClasses = null;
            for (AnonymousElementProvider provider : Extensions.getExtensions(AnonymousElementProvider.EP_NAME)) {
                anonymousClasses = provider.getAnonymousElements(psiClass);
                if (anonymousClasses.length > 0) {
                    break;
                }
            }
            if (anonymousClasses == null) {
                return psiClass;
            }

            if (index >= 0 && index < anonymousClasses.length) {
                current = anonymousClasses[index];
            } else {
                return (PsiClass) current;
            }
        }

        return (PsiClass) current;
    }

    /**
     * Given a {@link PsiAnnotationMemberValue}, find the mapping of child PsiElements and PsiClasses that they point to. If the given
     * {@link PsiAnnotationMemberValue} is a {@link PsiArrayInitializerMemberValue}, the returned map may return more than one entry.
     * If the {@link PsiAnnotationMemberValue} is a {@link PsiClassObjectAccessExpression}, {@link PsiReferenceExpression}, or
     * {@link PsiLiteralExpressionImpl}, only a single entry will be returned in the map. If target classes is any other type, an empty map
     * will be returned. An empty map will also be returned if the given {@link PsiAnnotationMemberValue} is null.
     *
     * @param targetClasses The {@link PsiAnnotationMemberValue} to check.
     * @return The {@link PsiElement} to {@link PsiClass} mapping that is defined in the given {@link PsiAnnotationMemberValue}.
     */
    @NotNull
    @Contract(pure = true)
    public static Map<PsiElement, PsiClass> resolveGenericClass(@Nullable PsiAnnotationMemberValue targetClasses) {
        final Map<PsiElement, PsiClass> map = Maps.newHashMap();

        if (targetClasses instanceof PsiArrayInitializerMemberValue) {
            final PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue) targetClasses).getInitializers();
            for (PsiAnnotationMemberValue initializer : initializers) {
                final PsiClass psiClass = resolveGenericClass((PsiElement) initializer);
                if (psiClass != null) {
                    map.put(initializer, psiClass);
                }
            }
        } else {
            final PsiClass psiClass = resolveGenericClass((PsiElement) targetClasses);
            if (psiClass != null) {
                map.put(targetClasses, psiClass);
            }
        }
        return map;
    }

    /**
     * Given a single PsiElement of type {@link PsiClassObjectAccessExpression}, {@link PsiReferenceExpression}, or
     * {@link PsiLiteralExpressionImpl}, find the PsiClass that the element points to. If the element is not one of these types,
     * return null. Also return null if the given element is null, or if no PsiClass could be found.
     *
     * @param element The element to check.
     * @return The PsiClass which the given element is referring to.
     */
    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiClass resolveGenericClass(@Nullable PsiElement element) {
        if (element instanceof PsiClassObjectAccessExpressionImpl) {
            final PsiClassObjectAccessExpressionImpl expression = (PsiClassObjectAccessExpressionImpl) element;

            final PsiType type = expression.getType();
            if (!(type instanceof PsiImmediateClassType)) {
                return null;
            }

            final PsiSubstitutor substitutor = ((PsiImmediateClassType) type).resolveGenerics().getSubstitutor();
            final Map<PsiTypeParameter, PsiType> substitutionMap = substitutor.getSubstitutionMap();

            final Set<Map.Entry<PsiTypeParameter, PsiType>> entries = substitutionMap.entrySet();
            if (entries.size() != 1) {
                return null;
            }
            final PsiClassReferenceType value = (PsiClassReferenceType) entries.iterator().next().getValue();

            return value.resolve();
        } else if (element instanceof PsiReferenceExpression) {
            // We need to find what value the reference expression is set to
            final PsiReferenceExpression expression = (PsiReferenceExpression) element;

            final PsiElement resolveEl = expression.resolve();
            if (resolveEl == null) {
                return null;
            }

            if (!(resolveEl instanceof PsiField)) {
                return null;
            }

            final PsiField resolveField = (PsiField) resolveEl;
            final ASTNode childByType = resolveField.getNode().findChildByType(ElementType.LITERAL_EXPRESSION);
            if (childByType == null) {
                return null;
            }

            final PsiElement psi = childByType.getPsi();
            if (psi == null) {
                return null;
            }

            if (!(psi instanceof PsiLiteralExpressionImpl)) {
                return null;
            }

            final PsiLiteralExpressionImpl lit = (PsiLiteralExpressionImpl) psi;

            final String text = lit.getInnerText();
            return MixinUtils.getClassFromMixinTargetString(text, element.getProject());
        } else if (element instanceof PsiLiteralExpressionImpl) {
            final PsiLiteralExpressionImpl expression = (PsiLiteralExpressionImpl) element;
            return MixinUtils.getClassFromMixinTargetString(expression.getInnerText(), element.getProject());
        }

        return null;
    }

    /**
     * Given a {@link PsiElement}, being either a {@link PsiMethod} or {@link PsiField}, find the corresponding field or method that is
     * being shadowed. Returns a notnull Pair of nullable values. The first value is the {@link PsiElement} that is found, or null if not
     * found. The second value is the {@link ShadowError error} if there is an actual error in the code regarding shadow validity. If the
     * first value is notnull, the second value is always null. If the first value is null, the second value may not be null. If both values
     * are null, then that means this is simply not a valid shadow, there isn't a specific error in the code. This could be, for example,
     * if this method is called on a {@link PsiElement} that isn't a {@link PsiMember} or {@link PsiField}, or it isn't in a Mixin class,
     * or it isn't {@code @Shadow} annotated, or it is null.
     *
     * @param element The element to check.
     * @return The PsiElement that is being shadowed, or the error if there is an error in the code.
     */
    @NotNull
    @Contract(pure = true)
    public static Pair<PsiElement, ShadowError> getShadowedElement(@Nullable PsiElement element) {
        if (element == null) {
            return Pair.create(null, null);
        }

        if (!(element instanceof PsiModifierListOwner)) {
            return Pair.create(null, null);
        }


        final PsiAnnotation annotation = McPsiUtil.getAnnotation((PsiModifierListOwner) element, MixinConstants.Annotations.SHADOW);
        if (annotation == null) {
            return Pair.create(null, null);
        }

        final PsiClass containingClass = MixinUtils.getContainingMixinClass(element);
        if (containingClass == null) {
            return Pair.create(null, null);
        }

        final Map<PsiElement, PsiClass> allMixedClasses = MixinUtils.getAllMixedClasses(containingClass);
        if (allMixedClasses.isEmpty()) {
            return Pair.create(null, null);
        }

        if (element instanceof PsiField) {
            final PsiField field = (PsiField) element;

            for (Map.Entry<PsiElement, PsiClass> entry : allMixedClasses.entrySet()) {
                final PsiField resolveField = entry.getValue().findFieldByName(field.getNameIdentifier().getText(), false);
                if (resolveField == null) {
                    continue;
                }

                // TODO ERROR
                return Pair.create(resolveField, null);
            }
        } else if (element instanceof PsiMethod) {
            final PsiMethod method = (PsiMethod) element;

            for (Map.Entry<PsiElement, PsiClass> entry : allMixedClasses.entrySet()) {
                final PsiMethod[] methodsByName = entry.getValue().findMethodsByName(method.getName(), false);
                if (methodsByName.length == 0) {
                    continue;
                }

                final String methodAccessModifier = McPsiUtil.getMethodAccessModifier(method);
                // There are multiple
                final ArrayList<PsiMethod> validAccessMethods = new ArrayList<>(methodsByName.length);
                for (PsiMethod psiMethod : methodsByName) {
                    if (McPsiUtil.getMethodAccessModifier(psiMethod).equalsIgnoreCase(methodAccessModifier)) {
                        validAccessMethods.add(psiMethod);
                    }
                }
                final ArrayList<PsiMethod> validSignatureMethods = new ArrayList<>(methodsByName.length);
                for (PsiMethod psiMethod : methodsByName) {
                    if (McMethodUtil.areSignaturesEqualLightweight(
                        psiMethod.getSignature(PsiSubstitutor.EMPTY),
                        method.getSignature(PsiSubstitutor.EMPTY),
                        method.getName()
                    )) {
                        // Don't worry about the nullable because it's not a constructor.
                        final PsiType returnType = method.getReturnType();
                        final PsiType possibleReturnType = psiMethod.getReturnType();
                        if (returnType == null || possibleReturnType == null) {
                            continue;
                        }

                        final PsiType erasedReturnType = TypeConversionUtil.erasure(returnType);
                        final PsiType erasedPossibleReturnType = TypeConversionUtil.erasure(possibleReturnType);
                        final boolean areTypesAgreed = TypeConversionUtil.typesAgree(returnType, possibleReturnType, true);

                        if (erasedReturnType.equals(erasedPossibleReturnType)) {
                            validSignatureMethods.add(psiMethod);
                        }
                    }
                }
                if (validSignatureMethods.isEmpty()) {
                    // TODO ERROR
                    return Pair.create(null, new ShadowError());
                }

                for (Iterator<PsiMethod> iterator = validAccessMethods.iterator(); iterator.hasNext();) {
                    if (!validSignatureMethods.contains(iterator.next())) {
                        iterator.remove();
                    }
                }
                if (validAccessMethods.isEmpty()) {
                    // TODO ERROR
                    return Pair.create(null, new ShadowError());
                }

                return Pair.create(validAccessMethods.get(0), null);
            }
        }

        return Pair.create(null, null);
    }
}
