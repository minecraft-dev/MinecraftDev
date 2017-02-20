/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import java.util.List;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MethodSideOnlyInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid usage of @SideOnly in method declaration";
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
        return "A method in a class annotated for one side cannot be declared as being in the other side. For example, a class which is " +
            "annotated as @SideOnly(Side.SERVER) cannot contain a method which is annotated as @SideOnly(Side.CLIENT). Since a class that " +
            "is annotated with @SideOnly brings everything with it, @SideOnly annotated methods are usually useless";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        final Error error = (Error) infos[0];
        final PsiMethod method = (PsiMethod) infos[3];

        if (method.isWritable() && error == Error.METHOD_IN_WRONG_CLASS) {
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
                    return "Remove @SideOnly annotation from method";
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
            public void visitMethod(PsiMethod method) {
                final PsiClass psiClass = method.getContainingClass();
                if (psiClass == null) {
                    return;
                }

                if (!SideOnlyUtil.beginningCheck(method)) {
                    return;
                }

                final Side methodSide = SideOnlyUtil.checkMethod(method);

                final PsiType returnType = method.getReturnType();
                if (!(returnType instanceof PsiClassType)) {
                    return;
                }

                final PsiClass resolve = ((PsiClassType) returnType).resolve();
                if (resolve == null) {
                    return;
                }

                final Side returnSide = SideOnlyUtil.getSideForClass(resolve);
                if (returnSide != Side.NONE && returnSide != Side.INVALID && returnSide != methodSide &&
                    methodSide != Side.NONE && methodSide != Side.INVALID) {
                    registerMethodError(method, Error.RETURN_TYPE_ON_WRONG_METHOD, methodSide.getName(), returnSide.getName(), method);
                }

                final List<Pair<Side, PsiClass>> classHierarchySides = SideOnlyUtil.checkClassHierarchy(psiClass);

                for (Pair<Side, PsiClass> classHierarchySide : classHierarchySides) {
                    if (classHierarchySide.first != Side.NONE && classHierarchySide.first != Side.INVALID) {
                        if (methodSide != classHierarchySide.first && methodSide != Side.NONE && methodSide != Side.INVALID) {

                            registerMethodError(
                                method,
                                Error.METHOD_IN_WRONG_CLASS,
                                methodSide.getName(),
                                classHierarchySide.first.getName(),
                                method
                            );
                        }
                        if (returnSide != Side.NONE  && returnSide != Side.INVALID) {
                            if (returnSide != classHierarchySide.first) {

                                registerMethodError(
                                    method,
                                    Error.RETURN_TYPE_IN_WRONG_CLASS,
                                    classHierarchySide.first.getName(),
                                    returnSide.getName(),
                                    method
                                );
                            }
                        }
                        return;
                    }
                }
            }
        };
    }

    enum Error {
        METHOD_IN_WRONG_CLASS() {
            @Override
            String getErrorString(Object... infos) {
                return "Method annotated with " + infos[0] +
                    " cannot be declared inside a class annotated with " + infos[1] + ".";
            }
        },
        RETURN_TYPE_ON_WRONG_METHOD() {
            @Override
            String getErrorString(Object... infos) {
                return "Method annotated with " + infos[0] +
                    " cannot return a type annotated with " + infos[1] + ".";
            }
        },
        RETURN_TYPE_IN_WRONG_CLASS() {
            @Override
            String getErrorString(Object... infos) {
                return "Method in a class annotated with " + infos[0] +
                    " cannot return a type annotated with " + infos[1] + ".";
            }
        };

        abstract String getErrorString(Object... infos);
    }
}
