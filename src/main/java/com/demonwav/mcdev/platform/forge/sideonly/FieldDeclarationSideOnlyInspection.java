package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.PsiUtil;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FieldDeclarationSideOnlyInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid usage of @SideOnly in field declaration";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        boolean normal = (boolean) infos[3];

        if (normal) {
            return "Field annotated with " + infos[0] + " cannot be declared inside a class annotated with " + infos[1] + ".";
        } else {
            return "Field with type annotation " + infos[1] + " cannot be declared as " + infos[0] + ".";
        }
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "A field in a class annotated for one side cannot be declared as being in the other side. For example, a class which is " +
            "annotated as @SideOnly(Side.SERVER) cannot contain a field which is annotated as @SideOnly(Side.CLIENT). Since a class that " +
            "is annotated with @SideOnly brings everything with it, @SideOnly annotated fields are usually useless";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        return new InspectionGadgetsFix() {
            @Override
            protected void doFix(Project project, ProblemDescriptor descriptor) {
                PsiField field = (PsiField) infos[2];

                PsiModifierList list = field.getModifierList();
                if (list == null) {
                    return;
                }

                PsiAnnotation annotation = list.findAnnotation(SideOnlyUtil.SIDE_ONLY);
                if (annotation == null) {
                    return;
                }

                annotation.delete();
            }

            @Nls
            @NotNull
            @Override
            public String getName() {
                return "Remove @SideOnly annotation from field";
            }

            @Nls
            @NotNull
            @Override
            public String getFamilyName() {
                return getName();
            }
        };
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitField(PsiField field) {
                if (!(field instanceof PsiFieldImpl)) {
                    return;
                }

                PsiClass psiClass = PsiUtil.getClassOfElement(field);
                if (psiClass == null) {
                    return;
                }

                if (!SideOnlyUtil.beginningCheck(field)) {
                    return;
                }

                Side fieldSide = SideOnlyUtil.checkField((PsiFieldImpl) field);

                List<Pair<Side, PsiClass>> classHierarchySides = SideOnlyUtil.checkClassHierarchy(psiClass);
                Side classSide = SideOnlyUtil.getFirstSide(classHierarchySides);

                if (fieldSide != classSide) {
                    if (fieldSide != Side.NONE && classSide != Side.NONE && fieldSide != Side.INVALID && classSide != Side.INVALID) {
                        registerFieldError(field, fieldSide.getName(), classSide.getName(), field, true);
                    }
                }

                if (fieldSide == Side.INVALID || fieldSide == Side.NONE) {
                    return;
                }

                if (!(field.getType() instanceof PsiClassType)) {
                    return;
                }

                PsiClassType type = (PsiClassType) field.getType();
                PsiClass fieldClass = type.resolve();
                if (fieldClass == null) {
                    return;
                }

                List<Pair<Side, PsiClass>> fieldClassHierarchySides = SideOnlyUtil.checkClassHierarchy(fieldClass);
                Side fieldClassSide = SideOnlyUtil.getFirstSide(fieldClassHierarchySides);

                if (fieldClassSide == Side.NONE || fieldClassSide == Side.INVALID) {
                    return;
                }

                if (fieldClassSide != fieldSide) {
                    registerFieldError(field, fieldSide.getName(), fieldClassSide.getName(), field, false);
                }
            }
        };
    }
}
