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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalVariableDeclarationSideOnlyInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid usage of local variable declaration annotated with @SideOnly";
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
        return "A variable whose class declaration is annotated with @SideOnly for one side cannot be declared in a class" +
            " or method that does not match the same side.";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        final PsiClass variableClass = (PsiClass) infos[3];

        if (variableClass.isWritable()) {
            return new RemoveAnnotationInspectionGadgetsFix() {
                @Nullable
                @Override
                public PsiModifierListOwner getListOwner() {
                    return variableClass;
                }

                @Nls
                @NotNull
                @Override
                public String getName() {
                    return "Remove @SideOnly annotation from variable class declaration";
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
            public void visitLocalVariable(PsiLocalVariable variable) {
                final PsiClass psiClass = McPsiUtil.getClassOfElement(variable);
                if (psiClass == null) {
                    return;
                }

                if (!SideOnlyUtil.beginningCheck(variable)) {
                    return;
                }

                final PsiType type = variable.getType();

                if (!(type instanceof PsiClassType)) {
                    return;
                }

                final PsiClassType classType = (PsiClassType) type;

                final PsiClass variableClass = classType.resolve();
                if (variableClass == null) {
                    return;
                }

                final Side variableSide = SideOnlyUtil.getSideForClass(variableClass);
                if (variableSide == Side.NONE || variableSide == Side.INVALID) {
                    return;
                }

                final Side containingClassSide = SideOnlyUtil.getSideForClass(psiClass);
                final Side methodSide = SideOnlyUtil.checkElementInMethod(variable);

                boolean classAnnotated = false;

                if (containingClassSide != Side.NONE && containingClassSide != Side.INVALID) {
                    if (variableSide != containingClassSide) {
                        registerVariableError(
                            variable,
                            Error.VAR_CROSS_ANNOTATED_CLASS,
                            variableSide.getName(),
                            containingClassSide.getName(),
                            variableClass
                        );
                    }
                    classAnnotated = true;
                }

                if (methodSide == Side.INVALID) {
                    return;
                }

                if (variableSide != methodSide) {
                    if (methodSide == Side.NONE) {
                        if (!classAnnotated) {
                            registerVariableError(
                                variable,
                                Error.VAR_UNANNOTATED_METHOD,
                                variableSide.getName(),
                                methodSide.getName(),
                                variableClass
                            );
                        }
                    } else {
                        registerVariableError(
                            variable,
                            Error.VAR_CROSS_ANNOTATED_METHOD,
                            variableSide.getName(),
                            methodSide.getName(),
                            variableClass
                        );
                    }
                }
            }
        };
    }

    enum Error {
        VAR_CROSS_ANNOTATED_CLASS {
            @Override
            String getErrorString(Object... infos) {
                return "A local variable whose class is annotated with " + infos[0] + " cannot be used in a class annotated with " + infos[1];
            }
        },
        VAR_CROSS_ANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "A local variable whose class is annotated with " + infos[0] + " cannot be used in a method annotated with " + infos[1];
            }
        },
        VAR_UNANNOTATED_METHOD {
            @Override
            String getErrorString(Object... infos) {
                return "A local variable whose class is annotated with " + infos[0] + " cannot be used in an un-annotated method.";
            }
        };

        abstract String getErrorString(Object... infos);
    }
}
