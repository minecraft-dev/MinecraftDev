package com.demonwav.mcdev.platform.mixin.inspections;

import com.demonwav.mcdev.util.McPsiUtil;
import com.demonwav.mcdev.util.McMethodUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiImmediateClassType;
import com.intellij.psi.impl.source.tree.java.PsiClassObjectAccessExpressionImpl;
import com.intellij.psi.util.TypeConversionUtil;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShadowInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid Shadow";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        return ShadowMemberErrorMessages.formatError(infos);
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new ShadowVisitor();
    }

    static class ShadowVisitor extends BaseInspectionVisitor {

        @Override
        public void visitMethod(PsiMethod method) {
            final PsiAnnotation shadowAnnotation = method.getModifierList().findAnnotation("org.spongepowered.asm.mixin.Shadow");
            if (shadowAnnotation == null) {
                return;
            }
            final PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                return;
            }
            final PsiModifierList classModifierList = containingClass.getModifierList();
            if (classModifierList == null) {
                return;
            }
            final PsiAnnotation mixinAnnotation = classModifierList.findAnnotation("org.spongepowered.asm.mixin.Mixin");
            if (mixinAnnotation == null) {
                return;
            }
            // Success, we have a mixin!
            final PsiAnnotationMemberValue mixinClassValues = mixinAnnotation.findDeclaredAttributeValue("value");
            final PsiAnnotationMemberValue mixinStringTargets = mixinAnnotation.findDeclaredAttributeValue("targets");
            if (mixinClassValues == null && mixinStringTargets == null) {
                return; // we don't have a class this is validated in another area.
            }

            final PsiAnnotationMemberValue mixinTargetRemapValue = mixinAnnotation.findDeclaredAttributeValue("remap");
            boolean isTargetRemapped = true;
            if (mixinTargetRemapValue instanceof PsiLiteralExpression) {
                isTargetRemapped = (boolean) ((PsiLiteralExpression) mixinTargetRemapValue).getValue();
            }



            boolean shadowTargetRemapped = true;
            final PsiAnnotationMemberValue shadowRemap = shadowAnnotation.findDeclaredAttributeValue("remap");
            if (shadowRemap instanceof PsiLiteralExpression) {
                shadowTargetRemapped = (boolean) ((PsiLiteralExpression) shadowRemap).getValue();
            }
            final RemapStrategy remapStrategy = RemapStrategy.match(isTargetRemapped, shadowTargetRemapped);

            if (!remapStrategy.validateMethodCanBeUsedInMixin(method, containingClass, this, shadowAnnotation, mixinClassValues, mixinStringTargets)) {
                return; // Means we already got an error to display
            }

            // Now we can actually validate that the method is actually available in the target class
            remapStrategy.validateShadowExists(method, containingClass, this, shadowAnnotation, mixinClassValues, mixinStringTargets);
        }

        @Override
        public void visitField(PsiField field) {

        }

        enum RemapStrategy {
            BOTH(true, true) {
                @Override
                boolean validateMethodCanBeUsedInMixin(@NotNull PsiMethod method, @NotNull PsiClass containingClass, ShadowVisitor visitor, PsiAnnotation shadowAnnotation, @Nullable PsiAnnotationMemberValue mixinTargetClasses, @Nullable PsiAnnotationMemberValue mixinStringTargets) {
                    if (mixinTargetClasses != null && mixinStringTargets != null) {
                        visitor.registerError(shadowAnnotation, ShadowMemberErrorMessages.Keys.MULTI_TARGET, containingClass);
                        return false;
                    }
                    if (mixinStringTargets instanceof PsiArrayInitializerMemberValue) {
                        // Validate that multiple string targets is not in place
                        final PsiArrayInitializerMemberValue stringTargets = (PsiArrayInitializerMemberValue) mixinStringTargets;
                        final PsiAnnotationMemberValue[] initializers = stringTargets.getInitializers();
                        if (initializers.length > 1) {
                            visitor.registerError(shadowAnnotation, ShadowMemberErrorMessages.Keys.MULTI_TARGET_REMAPPED_TRUE, containingClass);
                            return false;
                        }
                        if (initializers.length <= 0) {
                            return false;
                        }
                        final PsiAnnotationMemberValue targetExpression = initializers[0];
                        if (!(targetExpression instanceof PsiClassObjectAccessExpression)) {
                            return false;
                        }
                        final PsiType type = ((PsiClassObjectAccessExpression) targetExpression).getType();
                        if (!(type instanceof PsiClassReferenceType)) {
                            return false;
                        }
                        return true;
                    }
                    if (mixinStringTargets instanceof PsiLiteralExpression) {
                        return true;
                    }
                    if (mixinTargetClasses instanceof PsiArrayInitializerMemberValue) {
                        final PsiArrayInitializerMemberValue classTargets = (PsiArrayInitializerMemberValue) mixinTargetClasses;
                        final PsiAnnotationMemberValue[] classes = classTargets.getInitializers();
                        if (classes.length > 1) {
                            visitor.registerError(shadowAnnotation, ShadowMemberErrorMessages.Keys.MULTI_TARGET_REMAPPED_TRUE, containingClass);
                            return false;
                        }
                        if (classes.length <= 0) {
                            return false;
                        }
                        if (!(classes[0] instanceof PsiClassObjectAccessExpression)) {
                            visitor.registerError(shadowAnnotation, ShadowMemberErrorMessages.Keys.MULTI_TARGET_REMAPPED_TRUE, containingClass);
                            return false;
                        }
                        return true;
                    }
                    if (!(mixinTargetClasses instanceof PsiClassObjectAccessExpression)) {
                        visitor.registerError(shadowAnnotation, ShadowMemberErrorMessages.Keys.MULTI_TARGET, containingClass);
                        return false;
                    }
                    return true;
                }

                @Override
                boolean validateShadowExists(PsiMethod method, PsiClass psiClass, ShadowVisitor visitor, PsiAnnotation shadowAnnotation, @Nullable PsiAnnotationMemberValue targetClasses, @Nullable PsiAnnotationMemberValue stringTargets) {
                    PsiClass targetedMixinClass = McPsiUtil.resolveGenericClass(targetClasses);
                    if (targetedMixinClass != null) {
                        isMethodValidFromClass(method, visitor, shadowAnnotation, targetedMixinClass);
                    }
                    return true;
                }
            },
            CLASS_NOT_SHADOW_TRUE(false, true) { // Shadows are not remapped if the target is not remapped.
                @Override
                boolean validateMethodCanBeUsedInMixin(@NotNull PsiMethod method, @NotNull PsiClass containingClass, ShadowVisitor visitor, PsiAnnotation shadowAnnotation, @Nullable PsiAnnotationMemberValue mixinTargetClasses, @Nullable PsiAnnotationMemberValue mixinStringTargets) {
                    return true; // Basically, we won't shadowing a method that is provided by the implementation,
                    // but rather a different thing that is adding the method. Minimal validation can be performed
                    // at this point since there are multiple ways one can add methods, even with a different transformation
                    // process, such as forge mapping.
                }

                @Override
                boolean validateShadowExists(PsiMethod method, PsiClass psiClass, ShadowVisitor visitor, PsiAnnotation shadowAnnotation, @Nullable PsiAnnotationMemberValue targetClasses, @Nullable PsiAnnotationMemberValue stringTargets) {
                    return false;
                }
            },
            CLASS_REMAPPED_SHADOW_NOT(true, false) {

                @Override
                boolean validateMethodCanBeUsedInMixin(@NotNull PsiMethod method, @NotNull PsiClass containingClass, ShadowVisitor visitor, PsiAnnotation shadowAnnotation, @Nullable PsiAnnotationMemberValue mixinTargetClasses, @Nullable PsiAnnotationMemberValue mixinStringTargets) {
                    return true; // Basically, we won't shadowing a method that is provided by the implementation,
                    // but rather a different thing that is adding the method. Minimal validation can be performed
                    // at this point since there are multiple ways one can add methods, even with a different transformation
                    // process, such as forge mapping.
                }

                @Override
                boolean validateShadowExists(PsiMethod method, PsiClass psiClass, ShadowVisitor visitor, PsiAnnotation shadowAnnotation, @Nullable PsiAnnotationMemberValue targetClasses, @Nullable PsiAnnotationMemberValue stringTargets) {
                    return false;
                }
            },
            NONE(false, false) { // Both are not remapped, no real
                @Override
                boolean validateMethodCanBeUsedInMixin(@NotNull PsiMethod method, @NotNull PsiClass containingClass, ShadowVisitor visitor, PsiAnnotation shadowAnnotation, @Nullable PsiAnnotationMemberValue mixinTargetClasses, @Nullable PsiAnnotationMemberValue mixinStringTargets) {
                    return true; // Basically, we won't shadowing a method that is provided by the implementation,
                    // but rather a different thing that is adding the method. Minimal validation can be performed
                    // at this point since there are multiple ways one can add methods, even with a different transformation
                    // process, such as forge mapping.
                }

                @Override
                boolean validateShadowExists(PsiMethod method, PsiClass psiClass, ShadowVisitor visitor, PsiAnnotation shadowAnnotation, @Nullable PsiAnnotationMemberValue targetClasses, @Nullable PsiAnnotationMemberValue stringTargets) {
                    return false;
                }
            },
            ;

            private static boolean isMethodValidFromClass(PsiMethod method, ShadowVisitor visitor, PsiAnnotation shadowAnnotation, PsiClass targetClass) {
                if (targetClass == null) {
                    return true;
                }
                final PsiAnnotationMemberValue shadowPrefixValue = shadowAnnotation.findDeclaredAttributeValue("prefix");
                if (shadowPrefixValue != null && !(shadowPrefixValue instanceof PsiLiteralExpression)) {
                    return true;
                }
                final String shadowPrefix = shadowPrefixValue == null ? "shadow$" : ((PsiLiteralExpression) shadowPrefixValue).getValue().toString();
                String shadowTargetMethodName = method.getName().replace(shadowPrefix, "");
                final PsiMethod[] methodsByName = targetClass.findMethodsByName(shadowTargetMethodName, false);
                if (methodsByName.length == 0) {
                    visitor.registerError(method, ShadowMemberErrorMessages.Keys.NO_SHADOW_METHOD_FOUND_WITH_REMAP, method.getName(), targetClass.getName());
                    return true;
                }
                final String methodAccessModifier = McPsiUtil.getMethodAccessModifier(method);
                // There are multiple
                List<PsiMethod> validAccessMethods = new ArrayList<>(methodsByName.length);
                for (PsiMethod psiMethod : methodsByName) {
                    if (McPsiUtil.getMethodAccessModifier(psiMethod).equalsIgnoreCase(methodAccessModifier)) {
                        validAccessMethods.add(psiMethod);
                    }
                }
                List<PsiMethod> validSignatureMethods = new ArrayList<>(methodsByName.length);
                for (PsiMethod psiMethod : methodsByName) {
                    if (McMethodUtil.areSignaturesEqualLightweight(psiMethod.getSignature(PsiSubstitutor.EMPTY), method.getSignature(PsiSubstitutor.EMPTY), shadowTargetMethodName)) {
                        // Don't worry about the nullable because it's not a constructor.
                        final PsiType returnType = method.getReturnType();
                        final PsiType possibleReturnType = psiMethod.getReturnType();
                        final PsiType erasedReturnType = TypeConversionUtil.erasure(returnType);
                        final PsiType erasedPossibleReturnType = TypeConversionUtil.erasure(possibleReturnType);
                        final boolean areTypesAgreed = TypeConversionUtil.typesAgree(returnType, possibleReturnType, true);

                        if (erasedReturnType.equals(erasedPossibleReturnType)) {
                            validSignatureMethods.add(psiMethod);
                        }
                    }
                }
                if (validSignatureMethods.isEmpty()) {
                    visitor.registerError(method, ShadowMemberErrorMessages.Keys.NO_MATCHING_METHODS_FOUND, method.getSignature(PsiSubstitutor.EMPTY).getName(), targetClass.getName(), methodsByName);
                    return true;
                }

                for (Iterator<PsiMethod> iterator = validAccessMethods.iterator(); iterator.hasNext(); ) {
                    if (!validSignatureMethods.contains(iterator.next())) {
                        iterator.remove();
                    }
                }
                if (validAccessMethods.isEmpty()) {
                    final PsiMethod psiMethod = validSignatureMethods.get(0);
                    final String probableAccessModifier = McPsiUtil.getMethodAccessModifier(psiMethod);
                    visitor.registerError(method, ShadowMemberErrorMessages.Keys.INVALID_ACCESSOR_ON_SHADOW_METHOD, methodAccessModifier, probableAccessModifier);
                    return true;
                }
                return false;
            }

            final boolean targetRemap;
            final boolean shadowRemap;

            RemapStrategy(boolean targetRemap, boolean shadowRemap) {
                this.targetRemap = targetRemap;
                this.shadowRemap = shadowRemap;
            }

            static RemapStrategy match(boolean targetRemap, boolean shadowRemap) {
                for (RemapStrategy remapStrategy : RemapStrategy.values()) {
                    if (remapStrategy.targetRemap == targetRemap && remapStrategy.shadowRemap == shadowRemap) {
                        return remapStrategy;
                    }
                }
                return BOTH;
            }
            abstract boolean validateShadowExists(PsiMethod method,
                                                  PsiClass psiClass,
                                                  ShadowVisitor visitor,
                                                  PsiAnnotation shadowAnnotation,
                                                  @Nullable PsiAnnotationMemberValue targetClasses,
                                                  @Nullable PsiAnnotationMemberValue stringTargets);

            abstract boolean validateMethodCanBeUsedInMixin(PsiMethod method,
                                                            PsiClass containingClass,
                                                            ShadowVisitor visitor,
                                                            PsiAnnotation shadowAnnotation,
                                                            @Nullable PsiAnnotationMemberValue mixinTargetClasses,
                                                            @Nullable PsiAnnotationMemberValue mixinStringTargets);
        }

    }

    static final class ShadowMemberErrorMessages {

        static String formatError(Object... args) {
            if (args.length == 0) {
                return "Error";
            }

            final Object first = args[0];
            if (!(first instanceof String)) {
                return "Error";
            }
            return FORMATTERS.stream()
                    .filter(formatter -> formatter.matches((String) first))
                    .findFirst()
                    .map(found -> found.formatMessage(args))
                    .orElse("Error");
        }

        static final List<ShadowMemberErrorMessageFormatter> FORMATTERS = ImmutableList.<ShadowMemberErrorMessageFormatter>builder()
                .add(new ShadowMemberErrorMessageFormatter(Keys.MULTI_TARGET) {
                    @Override
                    String formatMessage(Object... args) {
                        Preconditions.checkArgument(args[0].equals(Keys.MULTI_TARGET));
                        final PsiClass containingClass = (PsiClass) args[1];
                        final String containingName = containingClass.getName();

                        return "Cannot have a shadow when " + containingName + " is mixing into multiple targets";
                    }
                })
                .add(new ShadowMemberErrorMessageFormatter(Keys.MULTI_TARGET_REMAPPED_TRUE) {
                    @Override
                    String formatMessage(Object... args) {
                        Preconditions.checkArgument(args[0].equals(Keys.MULTI_TARGET_REMAPPED_TRUE));
                        final PsiClass containingClass = (PsiClass) args[1];
                        final String containingName = containingClass.getName();

                        return "Cannot have a shadow when " + containingName + " is mixing into multiple targets while targets are remapped";
                    }
                })
                .add(new ShadowMemberErrorMessageFormatter(Keys.NO_SHADOW_METHOD_FOUND_WITH_REMAP) {
                    @Override
                    String formatMessage(Object... args) {
                        Preconditions.checkArgument(args[0].equals(Keys.NO_SHADOW_METHOD_FOUND_WITH_REMAP));
                        final String methodName = (String) args[1];
                        final String targetClassName = (String) args[2];

                        return "No method found by the name: " + methodName + " in target class: " + targetClassName;
                    }
                })
                .add(new ShadowMemberErrorMessageFormatter(Keys.NO_MATCHING_METHODS_FOUND) {
                    @Override
                    String formatMessage(Object... args) {
                        Preconditions.checkArgument(args[0].equals(Keys.NO_MATCHING_METHODS_FOUND));
                        final String methodName = (String) args[1];
                        final String targetClassName = (String) args[2];
                        final PsiMethod[] foundMethods = (PsiMethod[]) args[3];
                        return "No methods found matching signature: " + methodName + " in target class: " + targetClassName + ".";
                    }
                })
                .add(new ShadowMemberErrorMessageFormatter(Keys.INVALID_ACCESSOR_ON_SHADOW_METHOD) {
                    @Override
                    String formatMessage(Object... args) {
                        Preconditions.checkArgument(args[0].equals(Keys.INVALID_ACCESSOR_ON_SHADOW_METHOD));
                        final String current = (String) args[1];
                        final String expected = (String) args[2];
                        return "Method has invalid access modifiers, has: " + current + " but target method has: " + expected + ".";
                    }
                })
                .build();

        static final class Keys {
            static final String MULTI_TARGET_REMAPPED_TRUE = "multi-target-remapped-true";
            static final String MULTI_TARGET = "multi-target";
            static final String NO_SHADOW_METHOD_FOUND_WITH_REMAP = "no-shadow-method-found-in-obfuscated-environment";
            static final String NO_MATCHING_METHODS_FOUND = "methods-found-but-parameter-mismatch";
            static final String INVALID_ACCESSOR_ON_SHADOW_METHOD = "invalid-method-accessor-modifier-on-shadow";
        }

    }

    static abstract class ShadowMemberErrorMessageFormatter {

        final String key;

        ShadowMemberErrorMessageFormatter(String key) {
            this.key = key;
        }

        boolean matches(String key) {
            return this.key.equalsIgnoreCase(key);
        }

        abstract String formatMessage(Object... args);
    }
}
