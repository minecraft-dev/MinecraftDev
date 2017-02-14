/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspections;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mcp.McpModule;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mcp.srg.SrgMap;
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.platform.mixin.util.ShadowError;
import com.demonwav.mcdev.platform.mixin.util.ShadowError.Key;
import com.demonwav.mcdev.platform.mixin.util.ShadowedMembers;
import com.demonwav.mcdev.util.McPsiClass;
import com.demonwav.mcdev.util.McPsiUtil;

import com.google.common.collect.Lists;
import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.ProblemDescriptorImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShadowInspection extends BaseJavaBatchLocalInspectionTool {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid @Shadow";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "Detects issues regarding @Shadow'ed fields and methods.";
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkMethod(@NotNull PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (method.getNameIdentifier() != null) {
            return checkMember(method, method.getNameIdentifier());
        }
        return ProblemDescriptor.EMPTY_ARRAY;
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkField(@NotNull PsiField field, @NotNull InspectionManager manager, boolean isOnTheFly) {
        return checkMember(field, field.getNameIdentifier());
    }

    private ProblemDescriptor[] checkMember(@NotNull PsiMember member, @NotNull PsiIdentifier identifier) {
        final Info info = getInfo(member);
        if (info == null) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        McpModule mcpModule = null;
        final Module module = ModuleUtilCore.findModuleForPsiElement(member);
        if (module != null) {
            final MinecraftModule instance = MinecraftModule.getInstance(module);
            if (instance != null) {
                if (instance.isOfType(McpModuleType.getInstance())) {
                    mcpModule = instance.getModuleOfType(McpModuleType.getInstance());
                }
            }
        }

        List<ShadowError> errors = info.strategy.validateShadowMemberInMixin(member, mcpModule, info.containingClass, info.shadowAnnotation, info.psiClassMap);

        return generateProblemDescriptors(errors, identifier);
    }

    private ProblemDescriptor[] generateProblemDescriptors(List<ShadowError> errors, PsiIdentifier identifier) {
        if (errors.size() == 0) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        final List<ProblemDescriptorImpl> collect = errors.stream().map(e -> new ProblemDescriptorImpl(
            identifier,
            identifier.getNextSibling(),
            e.formatError(),
            new LocalQuickFix[] { e.fixError() },
            e.getLevel().getHighlightType(),
            false,
            identifier.getTextRange(),
            false
        )).collect(Collectors.toList());

        ProblemDescriptor[] descriptors = new ProblemDescriptor[collect.size()];
        collect.toArray(descriptors);
        return descriptors;
    }

    @Nullable
    @Contract(value = "null -> null", pure = true)
    private Info getInfo(@Nullable PsiModifierListOwner owner) {
        if (!MixinUtils.isMixinModule(owner)) {
            return null;
        }

        final PsiAnnotation shadowAnnotation = McPsiClass.findAnnotation(owner, Annotations.SHADOW);
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
        public final RemapStrategy strategy;
        public final PsiClass containingClass;
        public final PsiAnnotation shadowAnnotation;
        public final Map<PsiElement, PsiClass> psiClassMap;
        public Info(RemapStrategy strategy, PsiClass containingClass, PsiAnnotation shadowAnnotation, Map<PsiElement, PsiClass> psiClassMap) {
            this.strategy = strategy;
            this.containingClass = containingClass;
            this.shadowAnnotation = shadowAnnotation;
            this.psiClassMap = psiClassMap;
        }
    }

    private enum RemapStrategy {
        BOTH(true, true) {
            @Override
            List<ShadowError> validateShadowMemberInMixin(@NotNull PsiMember member,
                                                          @Nullable McpModule mcpModule,
                                                          @NotNull PsiClass containingClass,
                                                          PsiAnnotation shadowAnnotation,
                                                          @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                final ArrayList<ShadowError> errors = Lists.newArrayList();
                final ShadowedMembers shadowedMembers = isMemberValidFromClass(member);
                errors.addAll(shadowedMembers.getErrors());

                if (mixedClasses.size() > 1) {
                    SrgMap srgMap = null;
                    if (mcpModule != null) {
                        srgMap = mcpModule.getSrgManager().getSrgMapNow();
                    }

                    if (srgMap == null) {
                        // just give up validation, since we have no mcp data
                        errors.add(
                            ShadowError.builder()
                                .setError(Key.MULTI_TARGET_CLASS_REMAPPED_TRUE)
                                .addContext(containingClass)
                                .build()
                        );
                        return errors;
                    }

                    boolean isGood = true;
                    String lastName = null;
                    for (PsiElement element : shadowedMembers.getTargets()) {
                        final String name;
                        if (element instanceof PsiMethod) {
                            name = srgMap.findMethodMcpToSrg(SrgMap.toString((PsiMethod) element));
                        } else {
                            name = srgMap.findFieldMcpToSrg(SrgMap.toString((PsiField) element));
                        }

                        if (name != null) {
                            isGood = false;
                        }

                        if (lastName == null) {
                            lastName = name;
                        } else {
                            if (!lastName.equals(name)) {
                                isGood = false;
                            }
                        }
                    }

                    if (!isGood) {
                        errors.add(
                            ShadowError.builder()
                                .setError(Key.MULTI_TARGET_CLASS_REMAPPED_TRUE)
                                .addContext(containingClass)
                                .build()
                        );
                    }
                }
                return errors;
            }
        },
        CLASS_NOT_SHADOW_REMAPPED(false, true) { // Shadows are not remapped if the target is not remapped.
            @Override
            List<ShadowError> validateShadowMemberInMixin(@NotNull PsiMember member,
                                                          @Nullable McpModule mcpModule,
                                                          @NotNull PsiClass containingClass,
                                                          PsiAnnotation shadowAnnotation,
                                                          @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                final ArrayList<ShadowError> errors = Lists.newArrayList();
                if (mixedClasses.size() > 1) {
                    errors.add(
                        ShadowError.builder()
                            .setError(Key.MULTI_TARGET_SHADOW_REMAPPED_TRUE)
                            .addContext(containingClass)
                            .build()
                    );
                }
                return errors;
            }
        },
        CLASS_REMAPPED_SHADOW_NOT(true, false) {
            @Override
            List<ShadowError> validateShadowMemberInMixin(@NotNull PsiMember member,
                                                          @Nullable McpModule mcpModule,
                                                          @NotNull PsiClass containingClass,
                                                          PsiAnnotation shadowAnnotation,
                                                          @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                final ArrayList<ShadowError> errors = Lists.newArrayList();
                if (mixedClasses.size() > 1) {
                    errors.add(
                        ShadowError.builder()
                            .setError(Key.MULTI_TARGET_CLASS_REMAPPED_TRUE)
                            .addContext(containingClass)
                            .build()
                    );
                }
                return errors;
            }
        },
        NONE(false, false) { // Both are not remapped
            @Override
            List<ShadowError> validateShadowMemberInMixin(@NotNull PsiMember member,
                                                          @Nullable McpModule mcpModule,
                                                          @NotNull PsiClass containingClass,
                                                          PsiAnnotation shadowAnnotation,
                                                          @NotNull Map<PsiElement, PsiClass> mixedClasses) {
                // Basically, we aren't shadowing a method that is provided by the implementation,
                // but rather a different thing that is adding the method. Minimal validation can be performed
                // at this point since there are multiple ways one can add methods, even with a different transformation
                // process, such as forge mapping.
                return Lists.newArrayList();
            }
        },
        ;

        @NotNull
        private static ShadowedMembers isMemberValidFromClass(PsiMember member) {
            return MixinUtils.getShadowedElement(member);
        }

        final boolean targetRemap;
        final boolean shadowRemap;

        RemapStrategy(boolean targetRemap, boolean shadowRemap) {
            this.targetRemap = targetRemap;
            this.shadowRemap = shadowRemap;
        }

        public static RemapStrategy match(boolean targetRemap, boolean shadowRemap) {
            for (RemapStrategy remapStrategy : RemapStrategy.values()) {
                if (remapStrategy.targetRemap == targetRemap && remapStrategy.shadowRemap == shadowRemap) {
                    return remapStrategy;
                }
            }
            return BOTH;
        }

        abstract List<ShadowError> validateShadowMemberInMixin(PsiMember member,
                                                               @Nullable McpModule mcpModule,
                                                               PsiClass psiClass,
                                                               PsiAnnotation shadowAnnotation,
                                                               @NotNull Map<PsiElement, PsiClass> mixedClasses);
    }
}
