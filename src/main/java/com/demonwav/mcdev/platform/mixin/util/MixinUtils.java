/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mcp.util.McpUtil;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;
import com.demonwav.mcdev.platform.mixin.util.ShadowError.Key;
import com.demonwav.mcdev.platform.mixin.util.ShadowError.Level;
import com.demonwav.mcdev.util.McMethodUtil;
import com.demonwav.mcdev.util.McPsiUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.ElementType;
import com.intellij.psi.impl.source.tree.java.PsiClassObjectAccessExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        return instance != null && instance.isOfType(MixinModuleType.INSTANCE);
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
            if (!(type instanceof PsiClassType)) {
                return null;
            }

            final PsiSubstitutor substitutor = ((PsiClassType) type).resolveGenerics().getSubstitutor();
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
            return McpUtil.getClassFromString(text, element.getProject());
        } else if (element instanceof PsiLiteralExpressionImpl) {
            final PsiLiteralExpressionImpl expression = (PsiLiteralExpressionImpl) element;
            return McpUtil.getClassFromString(expression.getInnerText(), element.getProject());
        }

        return null;
    }

    /**
     * Given a {@link PsiElement}, being either a {@link PsiMethod} or {@link PsiField}, find the corresponding field(s) or method(s) that
     * is being shadowed, and any errors with the shadow that may exist.
     *
     * @param element The element to check.
     * @return The PsiElement that is being shadowed, and the errors if there are errors in the code.
     */
    @NotNull
    @Contract(pure = true)
    public static ShadowedMembers getShadowedElement(@Nullable PsiElement element) {
        if (element == null) {
            return ShadowedMembers.EMPTY;
        }

        if (!(element instanceof PsiModifierListOwner)) {
            return ShadowedMembers.EMPTY;
        }

        final PsiAnnotation annotation = McPsiUtil.getAnnotation((PsiModifierListOwner) element, MixinConstants.Annotations.SHADOW);
        if (annotation == null) {
            return ShadowedMembers.EMPTY;
        }

        final PsiClass containingClass = MixinUtils.getContainingMixinClass(element);
        if (containingClass == null) {
            return ShadowedMembers.create().addError(ShadowError.builder().setError(Key.CANNOT_FIND_MIXIN_TARGET).build());
        }

        final Map<PsiElement, PsiClass> allMixedClasses = MixinUtils.getAllMixedClasses(containingClass);
        if (allMixedClasses.isEmpty()) {
            return ShadowedMembers.create().addError(ShadowError.builder().setError(Key.NO_MIXIN_CLASS_TARGETS).build());
        }

        final PsiAnnotationMemberValue shadowPrefixValue = annotation.findDeclaredAttributeValue("prefix");
        final PsiAnnotationMemberValue shadowAliasValue = annotation.findAttributeValue("aliases");
        if (shadowAliasValue != null && !shadowAliasValue.getText().equals("{}")) {
            // TODO Handle aliases
            return ShadowedMembers.EMPTY;
        }

        if (shadowPrefixValue != null && !(shadowPrefixValue instanceof PsiLiteralExpression)) {
            // Don't handle case
            // TODO can we?
            return ShadowedMembers.EMPTY;
        }

        final String shadowPrefix;
        if (shadowPrefixValue == null) {
            shadowPrefix = "shadow$";
        } else {
            final Object value = ((PsiLiteralExpression) shadowPrefixValue).getValue();
            if (value != null) {
                shadowPrefix = value.toString();
            } else {
                shadowPrefix = "";
            }
        }
        String name = ((PsiNamedElement) element).getName();
        assert name != null;
        String shadowTargetName = name.replace(shadowPrefix, "");

//        List<String> aliases = Lists.newArrayList();
//        if (shadowAliasValue instanceof PsiArrayInitializerMemberValueImpl) {
//            final PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValueImpl) shadowAliasValue).getInitializers();
//            for (PsiAnnotationMemberValue initializer : initializers) {
//                if (initializer instanceof PsiLiteralExpressionImpl) {
//                    String text = ((PsiLiteralExpressionImpl) initializer).getInnerText();
//                    if (text != null) {
//                        aliases.add(text);
//                    }
//                }
//            }
//        } else if (shadowAliasValue instanceof PsiLiteralExpressionImpl) {
//            String text = ((PsiLiteralExpressionImpl) shadowAliasValue).getInnerText();
//            if (text != null) {
//                aliases.add(text);
//            }
//        } // else: Ignore aliases

//        if (aliases.contains("this$0")) {
//            // Don't handle this case
//            // TODO can we?
//            return ShadowedMembers.EMPTY;
//        }

        if (element instanceof PsiField) {
            final PsiField field = (PsiField) element;

            final List<PsiField> resolveFields = Lists.newArrayList();
            final List<ShadowError> errors = Lists.newArrayList();

            for (Map.Entry<PsiElement, PsiClass> entry : allMixedClasses.entrySet()) {
                PsiField resolveField = entry.getValue().findFieldByName(shadowTargetName, true);
                if (resolveField == null) {
//                    if (!aliases.isEmpty()) {
//                        for (String alias : aliases) {
//                            resolveField = entry.getValue().findFieldByName(alias, true);
//                            if (resolveField != null) {
//                                break;
//                            }
//                        }
//                        if (resolveField == null) {
//                            continue;
//                        }
//                    }
                    continue;
                }

                resolveFields.add(resolveField);
                final String fieldAccessModifier = McPsiUtil.getAccessModifier(field);
                final String neededAccessModifier = McPsiUtil.getAccessModifier(resolveField);

                if (!neededAccessModifier.equals(fieldAccessModifier)) {
                    errors.add(ShadowError.builder()
                        .setLevel(Level.SOFT_WARNING)
                        .setError(Key.INVALID_ACCESSOR_ON_SHADOW_FIELD)
                        .addContext(fieldAccessModifier)
                        .addContext(neededAccessModifier)
                        .addContext(field)
                        .build());
                }

                if (!field.getType().equals(resolveField.getType())) {
                    errors.add(ShadowError.builder()
                        .setError(Key.INVALID_FIELD_TYPE)
                        .addContext(field.getType().getCanonicalText())
                        .addContext(resolveField.getType().getCanonicalText())
                        .addContext(field)
                        .addContext(resolveField.getType())
                        .build());
                }

                final PsiModifierList modifierList = field.getModifierList();
                if (resolveField.hasModifierProperty(PsiModifier.FINAL)) {
                    if (modifierList != null && modifierList.findAnnotation(MixinConstants.Annotations.FINAL) == null) {
                        errors.add(ShadowError.builder()
                            .setError(Key.NO_FINAL_ANNOTATION_WITH_FINAL_TARGET)
                            .addContext(field)
                            .build());
                    }
                }
            }

            if (resolveFields.isEmpty()) {
                errors.add(ShadowError.builder()
                    .setError(Key.NO_SHADOW_FIELD_FOUND_WITH_REMAP)
                    .addContext(field.getName())
                    .addContext(allMixedClasses.entrySet().stream().map(e -> {
                        if (e.getValue() instanceof PsiAnonymousClass) {
                            return e.getKey().getText();
                        }
                        return e.getValue().getName();
                    }).collect(Collectors.joining(", ")))
                    .build());
            }
            return ShadowedMembers.create().addTargets(resolveFields).addErrors(errors);
        } else if (element instanceof PsiMethod) {
            final PsiMethod method = (PsiMethod) element;

            final List<PsiMethod> resolveMethods = Lists.newArrayList();
            final List<ShadowError> errors = Lists.newArrayList();

            for (Map.Entry<PsiElement, PsiClass> entry : allMixedClasses.entrySet()) {
                final PsiMethod[] methodsByName = entry.getValue().findMethodsByName(shadowTargetName, true);
                if (methodsByName.length == 0) {
                    continue;
                }

                final String methodAccessModifier = McPsiUtil.getAccessModifier(method);
                // There are multiple
                final ArrayList<PsiMethod> validAccessMethods = new ArrayList<>(methodsByName.length);
                for (PsiMethod psiMethod : methodsByName) {
                    final String targetMethodAccessModifier = McPsiUtil.getAccessModifier(psiMethod);
                    if (Objects.equals(targetMethodAccessModifier, PsiModifier.PRIVATE) && Objects.equals(methodAccessModifier, PsiModifier.PROTECTED)) {
                        validAccessMethods.add(psiMethod);
                    } else if (Objects.equals(targetMethodAccessModifier, methodAccessModifier)) {
                        validAccessMethods.add(psiMethod);
                    }
                }
                final ArrayList<PsiMethod> validSignatureMethods = new ArrayList<>(methodsByName.length);
                for (PsiMethod psiMethod : methodsByName) {
                    if (McMethodUtil.areSignaturesEqualLightweight(
                        psiMethod.getSignature(PsiSubstitutor.EMPTY),
                        method.getSignature(PsiSubstitutor.EMPTY),
                        shadowTargetName
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
                    continue;
                }

                validAccessMethods.removeIf(psiMethod -> !validSignatureMethods.contains(psiMethod));
                if (validAccessMethods.isEmpty()) {
                    final PsiMethod psiMethod = validSignatureMethods.get(0);
                    final String probableAccessModifier = McPsiUtil.getAccessModifier(psiMethod);
                    PsiMethod returnMethod = methodsByName.length > 0 ? methodsByName[0] : null;
                    errors.add(ShadowError.builder()
                        .setLevel(Level.SOFT_WARNING)
                        .setError(Key.INVALID_ACCESSOR_ON_SHADOW_METHOD)
                        .addContext(methodAccessModifier)
                        .addContext(probableAccessModifier)
                        .addContext(method)
                        .build());
                    continue;
                }

                resolveMethods.addAll(validAccessMethods);
            }

            if (resolveMethods.isEmpty()) {
                errors.add(ShadowError.builder()
                    .setError(Key.NO_SHADOW_METHOD_FOUND_WITH_REMAP)
                    .addContext(method.getName())
                    .addContext(allMixedClasses.values().stream().map(PsiNamedElement::getName).collect(Collectors.joining(", ")))
                    .build()
                );
            }

            return ShadowedMembers.create().addTargets(resolveMethods).addErrors(errors);
        }

        return ShadowedMembers.EMPTY;
    }

    /**
     * Checks if the given {@link PsiClass} is an accessor Mixin. Returns true if and only if:
     * <ol>
     *     <li>The given parameter, {@code psiClassType}, is not null.</li>
     *     <li>The class given is a Mixin.</li>
     *     <li>The class given is an interface.</li>
     *     <li>All Mixin targets are classes.</li>
     *     <li>All member methods are decorated with either {@code @Accessor} or {@code @Invoker}.</li>
     * </ol>
     *
     * @param psiClassType The {@link PsiClass} to check.
     * @return True if the above checks is satisfied.
     */
    @Contract(pure = true)
    public static boolean isAccessorMixin(@Nullable final PsiClassType psiClassType) {
        if (psiClassType == null) {
            return false;
        }

        final PsiClass psiClass = psiClassType.resolve();
        if (psiClass == null) {
            return false;
        }

        if (!psiClass.isInterface()) {
            return false;
        }

        if (!(psiClass instanceof PsiClassImpl)) {
            return false;
        }

        final Map<PsiElement, PsiClass> mixedClasses = getAllMixedClasses(psiClass);
        if (mixedClasses.isEmpty()) {
            return false;
        }

        for (Map.Entry<PsiElement, PsiClass> entries : mixedClasses.entrySet()) {
            if (entries.getValue().isInterface()) {
                return false;
            }
        }

        for (PsiMethod method : ((PsiClassImpl) psiClass).getOwnMethods()) {
            if (method.getModifierList().findAnnotation(MixinConstants.Annotations.ACCESSOR) != null) {
                continue;
            }

            if (method.getModifierList().findAnnotation(MixinConstants.Annotations.INVOKER) != null) {
                continue;
            }

            return false;
        }

        return true;
    }
}
