package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        return "Method annotated with " + infos[0] +
            " cannot be declared inside a class annotated with " + infos[1] + ".";
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
        PsiMethod method = (PsiMethod) infos[2];

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
                PsiClass psiClass = McPsiUtil.getClassOfElement(method);
                if (psiClass == null) {
                    return;
                }

                if (!SideOnlyUtil.beginningCheck(method)) {
                    return;
                }

                Side methodSide = SideOnlyUtil.checkMethod(method);
                if (methodSide == Side.INVALID || methodSide == Side.NONE) {
                    return;
                }

                List<Pair<Side, PsiClass>> classHierarchySides = SideOnlyUtil.checkClassHierarchy(psiClass);

                for (Pair<Side, PsiClass> classHierarchySide : classHierarchySides) {
                    if (classHierarchySide.first != Side.NONE && classHierarchySide.first != Side.INVALID) {
                        if (methodSide != classHierarchySide.first) {
                            registerMethodError(method, methodSide.getName(), classHierarchySide.first, method);
                        }
                        return;
                    }
                }
            }
        };
    }
}
