/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.demonwav.mcdev.platform.forge.util.ForgeConstants;
import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReferenceExpression;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MethodCallSideOnlyInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid usage of a @SideOnly method call";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        final Error error = (Error) infos[0];
        return error.getErrorString(SideOnlyUtil.getSubArray(infos));
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "Methods which are declared with a @SideOnly annotation can only be used in matching @SideOnly classes and methods.";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        final PsiMethod method = (PsiMethod) infos[3];

        if (method.isWritable()) {
            return new RemoveAnnotationInspectionGadgetsFix() {
                @Nullable
                @Override
                public PsiModifierListOwner getListOwner() {
                    return method;
                }

                @Nls
                @NotNull
                @Override
                public String getName() {
                    return "Remove @SideOnly annotation from method declaration";
                }
            };
        } else {
            return null;
        }
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                if (!SideOnlyUtil.beginningCheck(expression)) {
                    return;
                }

                final PsiReferenceExpression referenceExpression = expression.getMethodExpression();
                final PsiExpression qualifierExpression = referenceExpression.getQualifierExpression();

                // If this field is a @SidedProxy field, don't check. This is because people often are naughty and use the server impl as
                // the base class for their @SidedProxy class, and client extends it. this messes up our checks, so we will just assume the
                // right class is loaded for @SidedProxy's
                label: {
                    if (qualifierExpression instanceof PsiReferenceExpression) {
                        final PsiReferenceExpression qualifierRefExpression = (PsiReferenceExpression) qualifierExpression;
                        final PsiElement resolve = qualifierRefExpression.resolve();

                        if (!(resolve instanceof PsiField)) {
                            break label;
                        }

                        final PsiField resolveField = (PsiField) resolve;
                        final PsiModifierList resolveFieldModifierList = resolveField.getModifierList();
                        if (resolveFieldModifierList == null) {
                            break label;
                        }

                        if (resolveFieldModifierList.findAnnotation(ForgeConstants.SIDED_PROXY_ANNOTATION) == null) {
                            break label;
                        }

                        return;
                    }
                }

                final PsiElement declaration = referenceExpression.resolve();

                // We can't really do anything unless this is a PsiFieldImpl, which it should be, but to be safe,
                // check the type before we make the cast
                if (!(declaration instanceof PsiMethod)) {
                    return;
                }

                final PsiMethod method = (PsiMethod) declaration;
                Side elementSide = SideOnlyUtil.checkMethod(method);

                // Check the class(es) the element is declared in
                final PsiClass declarationContainingClass = McPsiUtil.getClassOfElement(declaration);
                if (declarationContainingClass == null) {
                    return;
                }

                final List<Pair<Side, PsiClass>> declarationClassHierarchySides = SideOnlyUtil.checkClassHierarchy(declarationContainingClass);

                Side declarationClassSide = SideOnlyUtil.getFirstSide(declarationClassHierarchySides);

                // The element inherits the @SideOnly from it's parent class if it doesn't explicitly set it itself
                boolean inherited = false;
                if (declarationClassSide != Side.NONE && (elementSide == Side.INVALID || elementSide == Side.NONE)) {
                    inherited = true;
                    elementSide = declarationClassSide;
                }

                if (elementSide == Side.INVALID || elementSide == Side.NONE) {
                    return;
                }

                // Check the class(es) the element is in
                final PsiClass containingClass = McPsiUtil.getClassOfElement(expression);
                if (containingClass == null) {
                    return;
                }

                final Side classSide = SideOnlyUtil.getSideForClass(containingClass);

                boolean classAnnotated = false;

                if (classSide != Side.NONE && classSide != Side.INVALID) {
                    if (classSide != elementSide) {
                        if (inherited) {
                            registerError(
                                referenceExpression.getElement(),
                                Error.ANNOTATED_CLASS_METHOD_IN_CROSS_ANNOTATED_CLASS_METHOD,
                                elementSide.getName(),
                                classSide.getName(),
                                method
                            );
                        } else {
                            registerError(
                                referenceExpression.getElement(),
                                Error.ANNOTATED_METHOD_IN_CROSS_ANNOTATED_CLASS_METHOD,
                                elementSide.getName(),
                                classSide.getName(),
                                method
                            );
                        }
                    }
                    classAnnotated = true;
                }

                // Check the method the element is in
                final Side methodSide = SideOnlyUtil.checkElementInMethod(expression);

                // Put error on for method
                if (elementSide != methodSide && methodSide != Side.INVALID) {
                    if (methodSide == Side.NONE) {
                        // If the class is properly annotated the method doesn't need to also be annotated
                        if (!classAnnotated) {
                            if (inherited) {
                                registerError(
                                    referenceExpression.getElement(),
                                    Error.ANNOTATED_CLASS_METHOD_IN_UNANNOTATED_METHOD,
                                    elementSide.getName(),
                                    null,
                                    method
                                );
                            } else {
                                registerError(
                                    referenceExpression.getElement(),
                                    Error.ANNOTATED_METHOD_IN_UNANNOTATED_METHOD,
                                    elementSide.getName(),
                                    null,
                                    method
                                );
                            }
                        }
                    } else {
                        if (inherited) {
                            registerError(
                                referenceExpression.getElement(),
                                Error.ANNOTATED_CLASS_METHOD_IN_CROSS_ANNOTATED_METHOD,
                                elementSide.getName(),
                                methodSide.getName(),
                                method
                            );
                        } else {
                            registerError(
                                referenceExpression.getElement(),
                                Error.ANNOTATED_METHOD_IN_CROSS_ANNOTATED_METHOD,
                                elementSide.getName(),
                                methodSide.getName(),
                                method
                            );
                        }
                    }
                }
            }
        };
    }

    enum Error {
        ANNOTATED_METHOD_IN_UNANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Method annotated with " + infos[0] + " cannot be referenced in an un-annotated method.";
            }
        },
        ANNOTATED_CLASS_METHOD_IN_UNANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Method declared in a class annotated with " + infos[0] + " cannot be referenced in an un-annotated method.";
            }
        },
        ANNOTATED_METHOD_IN_CROSS_ANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Method annotated with " + infos[0] + " cannot be referenced in a method annotated with " + infos[1] + ".";
            }
        },
        ANNOTATED_CLASS_METHOD_IN_CROSS_ANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Method declared in a class annotated with " + infos[0] +
                        " cannot be referenced in a method annotated with " + infos[1] + ".";
            }
        },
        ANNOTATED_METHOD_IN_CROSS_ANNOTATED_CLASS_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Method annotated with " + infos[0] + " cannot be referenced in a class annotated with " + infos[1] + ".";
            }
        },
        ANNOTATED_CLASS_METHOD_IN_CROSS_ANNOTATED_CLASS_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Method declared in a class annotated with " + infos[0] +
                        " cannot be referenced in a class annotated with " + infos[1] + ".";
            }
        };

        abstract String getErrorString(Object... infos);
    }
}
