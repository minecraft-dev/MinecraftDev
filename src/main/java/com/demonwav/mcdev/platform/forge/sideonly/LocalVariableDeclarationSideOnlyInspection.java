package com.demonwav.mcdev.platform.forge.sideonly;

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
        boolean isClassError = (boolean) infos[3];
        boolean isContainerAnnotated = (boolean) infos[4];

        if (isClassError) {
            return "A local variable whose class is annotated with " + infos[0] + " cannot be used in a class annotated with " + infos[1];
        } else {
            if (isContainerAnnotated) {
                return "A local variable whose class is annotated with " + infos[0] + " cannot be used in a method annotated with " + infos[1];
            } else {
                return "A local variable whose class is annotated with " + infos[0] + " cannot be used in an un-annotated method.";
            }
        }
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
        PsiClass variableClass = (PsiClass) infos[2];

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
                PsiClass psiClass = McPsiUtil.getClassOfElement(variable);
                if (psiClass == null) {
                    return;
                }

                if (!SideOnlyUtil.beginningCheck(variable)) {
                    return;
                }

                PsiType type = variable.getType();

                if (!(type instanceof PsiClassType)) {
                    return;
                }

                PsiClassType classType = (PsiClassType) type;

                PsiClass variableClass = classType.resolve();
                if (variableClass == null) {
                    return;
                }

                Side variableSide = SideOnlyUtil.getSideForClass(variableClass);
                if (variableSide == Side.NONE || variableSide == Side.INVALID) {
                    return;
                }

                Side containingClassSide = SideOnlyUtil.getSideForClass(psiClass);
                Side methodSide = SideOnlyUtil.checkElementInMethod(variable);

                boolean classAnnotated = false;

                if (containingClassSide != Side.NONE && containingClassSide != Side.INVALID) {
                    if (variableSide != containingClassSide) {
                        registerVariableError(variable, variableSide.getName(), containingClassSide.getName(), variableClass, true, true);
                    }
                    classAnnotated = true;
                }

                if (methodSide == Side.INVALID) {
                    return;
                }

                if (variableSide != methodSide) {
                    if (methodSide == Side.NONE) {
                        if (!classAnnotated) {
                            registerVariableError(variable, variableSide.getName(), methodSide.getName(), variableClass, false, false);
                        }
                    } else {
                        registerVariableError(variable, variableSide.getName(), methodSide.getName(), variableClass, false, true);
                    }
                }
            }
        };
    }
}
