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

import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VariableUseSideOnlyInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid usage of variable annotated with @SideOnly";
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
        return "Variables which are declared with a @SideOnly annotation can only be used in matching @SideOnly classes and methods.";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        PsiFieldImpl field = (PsiFieldImpl) infos[3];

        if (field.isWritable() && !(boolean) infos[4]) {
            return new RemoveAnnotationInspectionGadgetsFix() {
                @Nullable
                @Override
                public PsiModifierListOwner getListOwner() {
                    return field;
                }

                @Nls
                @NotNull
                @Override
                public String getName() {
                    return "Remove @SideOnly annotation from field declaration";
                }
            };
        }
        return null;
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitReferenceExpression(PsiReferenceExpression expression) {
                if (!SideOnlyUtil.beginningCheck(expression)) {
                    return;
                }

                final PsiElement declaration = expression.resolve();

                // We can't really do anything unless this is a PsiFieldImpl, which it should be, but to be safe,
                // check the type before we make the cast
                if (!(declaration instanceof PsiFieldImpl)) {
                    return;
                }

                final PsiFieldImpl field = (PsiFieldImpl) declaration;
                Side elementSide = SideOnlyUtil.checkField(field);

                // Check the class(es) the element is declared in
                final PsiClass declarationContainingClass = McPsiUtil.getClassOfElement(declaration);
                if (declarationContainingClass == null) {
                    return;
                }

                final List<Pair<Side, PsiClass>> declarationClassHierarchySides = SideOnlyUtil.checkClassHierarchy(declarationContainingClass);

                final Side declarationClassSide = SideOnlyUtil.getFirstSide(declarationClassHierarchySides);

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
                                expression.getElement(),
                                Error.ANNOTATED_CLASS_VAR_IN_CROSS_ANNOTATED_CLASS_METHOD,
                                elementSide.getName(),
                                classSide.getName(),
                                field
                            );
                        } else {
                            registerError(
                                expression.getElement(),
                                Error.ANNOTATED_VAR_IN_CROSS_ANNOTATED_CLASS_METHOD,
                                elementSide.getName(),
                                classSide.getName(),
                                field
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
                                    expression.getElement(),
                                    Error.ANNOTATED_CLASS_VAR_IN_UNANNOTATED_METHOD,
                                    elementSide.getName(),
                                    null,
                                    field
                                );
                            } else {
                                registerError(
                                    expression.getElement(),
                                    Error.ANNOTATED_VAR_IN_UNANNOTATED_METHOD,
                                    elementSide.getName(),
                                    null,
                                    field
                                );
                            }
                        }
                    } else {
                        if (inherited) {
                            registerError(
                                expression.getElement(),
                                Error.ANNOTATED_CLASS_VAR_IN_CROSS_ANNOTATED_METHOD,
                                elementSide.getName(),
                                methodSide.getName(),
                                field
                            );
                        } else {
                            registerError(
                                expression.getElement(),
                                Error.ANNOTATED_VAR_IN_CROSS_ANNOTATED_METHOD,
                                elementSide.getName(),
                                methodSide.getName(),
                                field
                            );
                        }
                    }
                }
            }
        };
    }

    enum Error {
        ANNOTATED_VAR_IN_UNANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Variable annotated with " + infos[0] + " cannot be referenced in an un-annotated method.";
            }
        },
        ANNOTATED_CLASS_VAR_IN_UNANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Variable declared in a class annotated with " + infos[0] + " cannot be referenced in an un-annotated method.";
            }
        },
        ANNOTATED_VAR_IN_CROSS_ANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Variable annotated with " + infos[0] + " cannot be referenced in a method annotated with " + infos[1] + ".";
            }
        },
        ANNOTATED_CLASS_VAR_IN_CROSS_ANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Variable declared in a class annotated with " + infos[0] +
                    " cannot be referenced in a method annotated with " + infos[1] + ".";
            }
        },
        ANNOTATED_VAR_IN_CROSS_ANNOTATED_CLASS_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Variable annotated with " + infos[0] + " cannot be referenced in a class annotated with " + infos[1] + ".";
            }
        },
        ANNOTATED_CLASS_VAR_IN_CROSS_ANNOTATED_CLASS_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "Variable declared in a class annotated with " + infos[0] +
                    " cannot be referenced in a class annotated with " + infos[1] + ".";
            }
        };

        abstract String getErrorString(Object... infos);
    }
}
