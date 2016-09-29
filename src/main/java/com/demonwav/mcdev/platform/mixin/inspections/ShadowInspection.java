package com.demonwav.mcdev.platform.mixin.inspections;

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.util.McMethodUtil;
import com.demonwav.mcdev.util.McPsiUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.TypeConversionUtil;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    private static class ShadowVisitor extends BaseInspectionVisitor {
        @Override
        public void visitMethod(PsiMethod method) {
            if (!MixinUtils.isMixinModule(method)) {
                return;
            }

            final PsiAnnotation shadowAnnotation = McPsiUtil.getAnnotation(method, Annotations.SHADOW);
            if (shadowAnnotation == null) {
                return;
            }

            final PsiClass containingClass = MixinUtils.getContainingMixinClass(method);
            if (containingClass == null) {
                return;
            }

            // Success, we have a mixin!
            final Map<PsiElement, PsiClass> psiClassMap = MixinUtils.getAllMixedClasses(containingClass);
            if (psiClassMap.isEmpty()) {
                return;
            }

            final PsiAnnotationMemberValue mixinTargetRemapValue = MixinUtils.getMixinAnnotationAttribute(containingClass, "remap");
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

            if (!remapStrategy.validateMethodCanBeUsedInMixin(method, containingClass, this, shadowAnnotation, psiClassMap)) {
                return; // Means we already got an error to display
            }

            // Now we can actually validate that the method is actually available in the target class
            remapStrategy.validateShadowExists(method, containingClass, this, shadowAnnotation, psiClassMap);
        }

        @Override
        public void visitField(PsiField field) {

        }

        enum RemapStrategy {
            BOTH(true, true) {
                @Override
                boolean validateMethodCanBeUsedInMixin(@NotNull PsiMethod method,
                                                       @NotNull PsiClass containingClass,
                                                       ShadowVisitor visitor,
                                                       PsiAnnotation shadowAnnotation,
                                                       @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    if (mixedClasses.size() > 1) {
                        visitor.registerError(shadowAnnotation, ShadowMemberErrorMessages.Key.MULTI_TARGET_CLASS_REMAPPED_TRUE, containingClass);
                        return false;
                    }
                    return true;
                }

                @Override
                boolean validateShadowExists(PsiMethod method,
                                             PsiClass psiClass,
                                             ShadowVisitor visitor,
                                             PsiAnnotation shadowAnnotation,
                                             @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    if (!mixedClasses.isEmpty()) {
                        isMethodValidFromClass(method, visitor, shadowAnnotation, mixedClasses);
                    }
                    return true;
                }
            },
            CLASS_NOT_SHADOW_REMAPPED(false, true) { // Shadows are not remapped if the target is not remapped.
                @Override
                boolean validateMethodCanBeUsedInMixin(@NotNull PsiMethod method,
                                                       @NotNull PsiClass containingClass,
                                                       ShadowVisitor visitor,
                                                       PsiAnnotation shadowAnnotation,
                                                       @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    if (mixedClasses.size() > 1) {
                        visitor.registerError(shadowAnnotation, ShadowMemberErrorMessages.Key.MULTI_TARGET_SHADOW_REMAPPED_TRUE, containingClass);
                        return false;
                    }
                    return true;
                }

                @Override
                boolean validateShadowExists(PsiMethod method,
                                             PsiClass psiClass,
                                             ShadowVisitor visitor,
                                             PsiAnnotation shadowAnnotation,
                                             @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return false;
                }
            },
            CLASS_REMAPPED_SHADOW_NOT(true, false) {
                @Override
                boolean validateMethodCanBeUsedInMixin(@NotNull PsiMethod method,
                                                       @NotNull PsiClass containingClass,
                                                       ShadowVisitor visitor,
                                                       PsiAnnotation shadowAnnotation,
                                                       @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    if (mixedClasses.size() > 1) {
                        visitor.registerError(shadowAnnotation, ShadowMemberErrorMessages.Key.MULTI_TARGET_CLASS_REMAPPED_TRUE, containingClass);
                        return false;
                    }
                    return true;
                }

                @Override
                boolean validateShadowExists(PsiMethod method,
                                             PsiClass psiClass,
                                             ShadowVisitor visitor,
                                             PsiAnnotation shadowAnnotation,
                                             @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return false;
                }
            },
            NONE(false, false) { // Both are not remapped
                @Override
                boolean validateMethodCanBeUsedInMixin(@NotNull PsiMethod method,
                                                       @NotNull PsiClass containingClass,
                                                       ShadowVisitor visitor,
                                                       PsiAnnotation shadowAnnotation,
                                                       @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return true;
                    // Basically, we aren't shadowing a method that is provided by the implementation,
                    // but rather a different thing that is adding the method. Minimal validation can be performed
                    // at this point since there are multiple ways one can add methods, even with a different transformation
                    // process, such as forge mapping.
                }

                @Override
                boolean validateShadowExists(PsiMethod method,
                                             PsiClass psiClass,
                                             ShadowVisitor visitor,
                                             PsiAnnotation shadowAnnotation,
                                             @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return false;
                }
            },
            ;

            private static boolean isMethodValidFromClass(PsiMethod method,
                                                          ShadowVisitor visitor,
                                                          PsiAnnotation shadowAnnotation,
                                                          Map<PsiElement, PsiClass> mixedClasses) {
                if (mixedClasses.isEmpty()) {
                    visitor.registerError(method, ShadowMemberErrorMessages.Key.CANNOT_FIND_MIXIN_TARGET);
                    return false;
                }

                final PsiAnnotationMemberValue shadowPrefixValue = shadowAnnotation.findDeclaredAttributeValue("prefix");
                if (shadowPrefixValue != null && !(shadowPrefixValue instanceof PsiLiteralExpression)) {
                    return true;
                }
                final String shadowPrefix = shadowPrefixValue == null ? "shadow$" : ((PsiLiteralExpression) shadowPrefixValue).getValue().toString();
                String shadowTargetMethodName = method.getName().replace(shadowPrefix, "");

                boolean error = false;
                for (Map.Entry<PsiElement, PsiClass> entry : mixedClasses.entrySet()) {
                    final PsiClass targetClass = entry.getValue();

                    final PsiMethod[] methodsByName = targetClass.findMethodsByName(shadowTargetMethodName, false);
                    if (methodsByName.length == 0) {
                        visitor.registerError(method, ShadowMemberErrorMessages.Key.NO_SHADOW_METHOD_FOUND_WITH_REMAP, method.getName(), targetClass.getName());
                        error = true;
                        continue;
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
                        if (McMethodUtil.areSignaturesEqualLightweight(
                                psiMethod.getSignature(PsiSubstitutor.EMPTY),
                                method.getSignature(PsiSubstitutor.EMPTY),
                                shadowTargetMethodName
                        )) {
                            // Don't worry about the nullable because it's not a constructor.
//                            final PsiType returnType = method.getReturnType();
//                            final PsiType possibleReturnType = psiMethod.getReturnType();
//                            final PsiType erasedReturnType = TypeConversionUtil.erasure(returnType);
//                            final PsiType erasedPossibleReturnType = TypeConversionUtil.erasure(possibleReturnType);
//                            final boolean areTypesAgreed = TypeConversionUtil.typesAgree(returnType, possibleReturnType, true);
//
//                            if (erasedReturnType.equals(erasedPossibleReturnType)) {
//                                validSignatureMethods.add(psiMethod);
//                            }
                        }
                    }
                    if (validSignatureMethods.isEmpty()) {
                        visitor.registerError(
                                method,
                                ShadowMemberErrorMessages.Key.NO_MATCHING_METHODS_FOUND,
                                method.getSignature(PsiSubstitutor.EMPTY).getName(),
                                targetClass.getName(),
                                methodsByName
                        );
                        error = true;
                        continue;
                    }

                    for (Iterator<PsiMethod> iterator = validAccessMethods.iterator(); iterator.hasNext(); ) {
                        if (!validSignatureMethods.contains(iterator.next())) {
                            iterator.remove();
                        }
                    }
                    if (validAccessMethods.isEmpty()) {
                        final PsiMethod psiMethod = validSignatureMethods.get(0);
                        final String probableAccessModifier = McPsiUtil.getMethodAccessModifier(psiMethod);
                        visitor.registerError(method, ShadowMemberErrorMessages.Key.INVALID_ACCESSOR_ON_SHADOW_METHOD, methodAccessModifier, probableAccessModifier);
                        error = true;
                    }
                }

                return error;
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
                                                  @NotNull Map<PsiElement, PsiClass> mixedClasses);

            abstract boolean validateMethodCanBeUsedInMixin(PsiMethod method,
                                                            PsiClass containingClass,
                                                            ShadowVisitor visitor,
                                                            PsiAnnotation shadowAnnotation,
                                                            @NotNull Map<PsiElement, PsiClass> mixedClasses);
        }

    }

    public static final class ShadowMemberErrorMessages {

        static String formatError(Object... args) {
            if (args.length == 0) {
                return "Error";
            }

            final Object first = args[0];
            if (!(first instanceof Key)) {
                return "Error";
            }
            return FORMATTERS.stream()
                    .filter(formatter -> formatter.matches((Key) first))
                    .findFirst()
                    .map(found -> found.formatMessage(args))
                    .orElse("Error");
        }

        static final List<ShadowMemberErrorMessageFormatter> FORMATTERS = ImmutableList.<ShadowMemberErrorMessageFormatter>builder()
            .add(new ShadowMemberErrorMessageFormatter(Key.MULTI_TARGET_CLASS_REMAPPED_TRUE) {
                @Override
                String formatMessage(Object... args) {
                    Preconditions.checkArgument(args[0].equals(Key.MULTI_TARGET_CLASS_REMAPPED_TRUE));
                    final PsiClass containingClass = (PsiClass) args[1];
                    final String containingName = containingClass.getName();

                    return "Cannot have a shadow when " + containingName + " is mixing into multiple remapped targets.";
                }
            })
            .add(new ShadowMemberErrorMessageFormatter(Key.MULTI_TARGET_SHADOW_REMAPPED_TRUE) {
                @Override
                String formatMessage(Object... args) {
                    Preconditions.checkArgument(args[0].equals(Key.MULTI_TARGET_SHADOW_REMAPPED_TRUE));
                    final PsiClass containingClass = (PsiClass) args[1];
                    final String containingName = containingClass.getName();

                    return "Cannot have a remapped shadow when " + containingName + " is mixing into multiple targets.";
                }
            })
            .add(new ShadowMemberErrorMessageFormatter(Key.NO_SHADOW_METHOD_FOUND_WITH_REMAP) {
                @Override
                String formatMessage(Object... args) {
                    Preconditions.checkArgument(args[0].equals(Key.NO_SHADOW_METHOD_FOUND_WITH_REMAP));
                    final String methodName = (String) args[1];
                    final String targetClassName = (String) args[2];

                    return "No method found by the name: " + methodName + " in target classes: " + targetClassName;
                }
            })
            .add(new ShadowMemberErrorMessageFormatter(Key.NO_MATCHING_METHODS_FOUND) {
                @Override
                String formatMessage(Object... args) {
                    Preconditions.checkArgument(args[0].equals(Key.NO_MATCHING_METHODS_FOUND));
                    final String methodName = (String) args[1];
                    final String targetClassName = (String) args[2];
                    final PsiMethod[] foundMethods = (PsiMethod[]) args[3];
                    return "No methods found matching signature: " + methodName + " in target classes: " + targetClassName + ".";
                }
            })
            .add(new ShadowMemberErrorMessageFormatter(Key.INVALID_ACCESSOR_ON_SHADOW_METHOD) {
                @Override
                String formatMessage(Object... args) {
                    Preconditions.checkArgument(args[0].equals(Key.INVALID_ACCESSOR_ON_SHADOW_METHOD));
                    final String current = (String) args[1];
                    final String expected = (String) args[2];
                    return "Method has invalid access modifiers, has: " + current + " but target method has: " + expected + ".";
                }
            })
            .add(new ShadowMemberErrorMessageFormatter(Key.CANNOT_FIND_MIXIN_TARGET) {
                @Override
                String formatMessage(Object... args) {
                    Preconditions.checkArgument(args[0].equals(Key.CANNOT_FIND_MIXIN_TARGET));
                    return "Cannot shadow nonexistent target: target class undefined";
                }
            })
            .add(new ShadowMemberErrorMessageFormatter(Key.NOT_MIXIN_CLASS) {
                @Override
                String formatMessage(Object... args) {
                    Preconditions.checkArgument(args[0].equals(Key.NOT_MIXIN_CLASS));
                    return "Cannot shadow anything in a non-@Mixin annotated class.";
                }
            })
            .add(new ShadowMemberErrorMessageFormatter(Key.NO_MIXIN_CLASS_TARGETS) {
                @Override
                String formatMessage(Object... args) {
                    Preconditions.checkArgument(args[0].equals(Key.NO_MIXIN_CLASS_TARGETS));
                    return "Cannot shadow anything when the Mixin class has no targets.";
                }
            })
            .build();

        public enum Key {
            MULTI_TARGET_CLASS_REMAPPED_TRUE,
            MULTI_TARGET_SHADOW_REMAPPED_TRUE,
            NO_SHADOW_METHOD_FOUND_WITH_REMAP,
            NO_MATCHING_METHODS_FOUND,
            INVALID_ACCESSOR_ON_SHADOW_METHOD,
            CANNOT_FIND_MIXIN_TARGET,
            NOT_MIXIN_CLASS,
            NO_MIXIN_CLASS_TARGETS
        }
    }

    static abstract class ShadowMemberErrorMessageFormatter {
        final ShadowMemberErrorMessages.Key key;

        ShadowMemberErrorMessageFormatter(ShadowMemberErrorMessages.Key key) {
            this.key = key;
        }

        boolean matches(ShadowMemberErrorMessages.Key key) {
            return this.key == key;
        }

        abstract String formatMessage(Object... args);
    }
}
