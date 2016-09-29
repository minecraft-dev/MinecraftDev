package com.demonwav.mcdev.platform.mixin.inspections;

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.platform.mixin.util.ShadowError;
import com.demonwav.mcdev.platform.mixin.util.ShadowError.Key;
import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        return ShadowError.Errors.formatError(infos);
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        return ShadowError.Errors.fixError(infos);
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new ShadowVisitor();
    }

    private static class ShadowVisitor extends BaseInspectionVisitor {
        @Override
        public void visitMethod(PsiMethod method) {
            final Info info = getInfo(method);
            if (info == null) {
                return;
            }

            if (!info.strategy.validateMemberCanBeUsedInMixin(method, info.containingClass, this, info.shadowAnnotation, info.psiClassMap)) {
                return; // Means we already got an error to display
            }

            // Now we can actually validate that the method is actually available in the target class
            info.strategy.validateShadowMethodExists(method, info.containingClass, this, info.shadowAnnotation, info.psiClassMap);
        }

        @Override
        public void visitField(PsiField field) {
            final Info info = getInfo(field);
            if (info == null) {
                return;
            }

            if (!info.strategy.validateMemberCanBeUsedInMixin(field, info.containingClass, this, info.shadowAnnotation, info.psiClassMap)) {
                return; // Means we already go an error to display
            }

            info.strategy.validateShadowFieldExists(field, info.containingClass, this, info.shadowAnnotation, info.psiClassMap);
        }

        @Nullable
        @Contract(value = "null -> null", pure = true)
        private Info getInfo(@Nullable PsiModifierListOwner owner) {
            if (!MixinUtils.isMixinModule(owner)) {
                return null;
            }

            final PsiAnnotation shadowAnnotation = McPsiUtil.getAnnotation(owner, Annotations.SHADOW);
            if (shadowAnnotation == null) {
                return null;
            }

            final PsiClass containingClass = MixinUtils.getContainingMixinClass(owner);
            if (containingClass == null) {
                return null;
            }

            // We have a mixin
            final Map<PsiElement, PsiClass> psiClassMap = MixinUtils.getAllMixedClasses(containingClass);
            if (psiClassMap.isEmpty()) {
                return null;
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

            return new Info(
                RemapStrategy.match(isTargetRemapped, shadowTargetRemapped),
                containingClass,
                shadowAnnotation,
                psiClassMap
            );
        }

        private static class Info {
            public RemapStrategy strategy;
            public PsiClass containingClass;
            public PsiAnnotation shadowAnnotation;
            public Map<PsiElement, PsiClass> psiClassMap;
            public Info(RemapStrategy strategy, PsiClass containingClass, PsiAnnotation shadowAnnotation, Map<PsiElement, PsiClass> psiClassMap) {
                this.strategy = strategy;
                this.containingClass = containingClass;
                this.shadowAnnotation = shadowAnnotation;
                this.psiClassMap = psiClassMap;
            }
        }

        enum RemapStrategy {
            BOTH(true, true) {
                @Override
                boolean validateMemberCanBeUsedInMixin(@NotNull PsiMember member,
                                                       @NotNull PsiClass containingClass,
                                                       ShadowVisitor visitor,
                                                       PsiAnnotation shadowAnnotation,
                                                       @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    if (mixedClasses.size() > 1) {
                        visitor.registerError(shadowAnnotation, Key.MULTI_TARGET_CLASS_REMAPPED_TRUE, containingClass);
                        return false;
                    }
                    return true;
                }

                @Override
                boolean validateShadowFieldExists(PsiField field,
                                                  PsiClass psiClass,
                                                  ShadowVisitor visitor,
                                                  PsiAnnotation shadowAnnotation,
                                                  @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    if (!mixedClasses.isEmpty()) {
                        isFieldValidFromClass(field, visitor);
                    }
                    return true;
                }

                @Override
                boolean validateShadowMethodExists(PsiMethod method,
                                                   PsiClass psiClass,
                                                   ShadowVisitor visitor,
                                                   PsiAnnotation shadowAnnotation,
                                                   @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    if (!mixedClasses.isEmpty()) {
                        isMethodValidFromClass(method, visitor);
                    }
                    return true;
                }
            },
            CLASS_NOT_SHADOW_REMAPPED(false, true) { // Shadows are not remapped if the target is not remapped.
                @Override
                boolean validateMemberCanBeUsedInMixin(@NotNull PsiMember member,
                                                       @NotNull PsiClass containingClass,
                                                       ShadowVisitor visitor,
                                                       PsiAnnotation shadowAnnotation,
                                                       @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    if (mixedClasses.size() > 1) {
                        visitor.registerError(shadowAnnotation, Key.MULTI_TARGET_SHADOW_REMAPPED_TRUE, containingClass);
                        return false;
                    }
                    return true;
                }

                @Override
                boolean validateShadowFieldExists(PsiField field,
                                                  PsiClass psiClass,
                                                  ShadowVisitor visitor,
                                                  PsiAnnotation shadowAnnotation,
                                                  @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return false;
                }

                @Override
                boolean validateShadowMethodExists(PsiMethod method,
                                                   PsiClass psiClass,
                                                   ShadowVisitor visitor,
                                                   PsiAnnotation shadowAnnotation,
                                                   @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return false;
                }
            },
            CLASS_REMAPPED_SHADOW_NOT(true, false) {
                @Override
                boolean validateMemberCanBeUsedInMixin(@NotNull PsiMember member,
                                                       @NotNull PsiClass containingClass,
                                                       ShadowVisitor visitor,
                                                       PsiAnnotation shadowAnnotation,
                                                       @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    if (mixedClasses.size() > 1) {
                        visitor.registerError(shadowAnnotation, Key.MULTI_TARGET_CLASS_REMAPPED_TRUE, containingClass);
                        return false;
                    }
                    return true;
                }

                @Override
                boolean validateShadowFieldExists(PsiField field,
                                                  PsiClass psiClass,
                                                  ShadowVisitor visitor,
                                                  PsiAnnotation shadowAnnotation,
                                                  @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return false;
                }

                @Override
                boolean validateShadowMethodExists(PsiMethod method,
                                                   PsiClass psiClass,
                                                   ShadowVisitor visitor,
                                                   PsiAnnotation shadowAnnotation,
                                                   @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return false;
                }
            },
            NONE(false, false) { // Both are not remapped
                @Override
                boolean validateMemberCanBeUsedInMixin(@NotNull PsiMember member,
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
                boolean validateShadowFieldExists(PsiField field,
                                                  PsiClass psiClass,
                                                  ShadowVisitor visitor,
                                                  PsiAnnotation shadowAnnotation,
                                                  @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return false;
                }

                @Override
                boolean validateShadowMethodExists(PsiMethod method,
                                                   PsiClass psiClass,
                                                   ShadowVisitor visitor,
                                                   PsiAnnotation shadowAnnotation,
                                                   @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                    return false;
                }
            },
            ;

            private static boolean isMethodValidFromClass(PsiMethod method,
                                                          ShadowVisitor visitor) {

                final Pair<PsiElement, ShadowError> shadowedElement = MixinUtils.getShadowedElement(method);
                if (shadowedElement.getSecond() != null) {
                    visitor.registerError(method, shadowedElement.getSecond().getErrorContextInfos());
                    return true;
                }
                return false;
            }

            private static boolean isFieldValidFromClass(PsiField field, ShadowVisitor visitor) {
                final Pair<PsiElement, ShadowError> shadowedElement = MixinUtils.getShadowedElement(field);
                if (shadowedElement.getSecond() != null) {
                    visitor.registerError(field, shadowedElement.getSecond().getErrorContextInfos());
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

            abstract boolean validateShadowMethodExists(PsiMethod method,
                                                        PsiClass psiClass,
                                                        ShadowVisitor visitor,
                                                        PsiAnnotation shadowAnnotation,
                                                        @NotNull Map<PsiElement, PsiClass> mixedClasses);

            abstract boolean validateShadowFieldExists(PsiField field,
                                                       PsiClass psiClass,
                                                       ShadowVisitor visitor,
                                                       PsiAnnotation shadowAnnotation,
                                                       @NotNull Map<PsiElement, PsiClass> mixedClasses);

            abstract boolean validateMemberCanBeUsedInMixin(PsiMember member,
                                                            PsiClass containingClass,
                                                            ShadowVisitor visitor,
                                                            PsiAnnotation shadowAnnotation,
                                                            @NotNull Map<PsiElement, PsiClass> mixedClasses);
        }
    }
}
