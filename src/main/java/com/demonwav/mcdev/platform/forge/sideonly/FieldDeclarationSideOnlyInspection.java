package com.demonwav.mcdev.platform.forge.sideonly;

import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        boolean matchContainingClass = (boolean) infos[4];


        if (matchContainingClass) {
            return "Field with type annotation " + infos[1] + " cannot be declared in an un-annotated class";
        } else if (normal) {
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
        PsiField field = (PsiField) infos[2];

        if (field.isWritable()) {
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
                    return "Remove @SideOnly annotation from field";
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
            public void visitField(PsiField field) {
                if (!(field instanceof PsiFieldImpl)) {
                    return;
                }

                PsiClass psiClass = McPsiUtil.getClassOfElement(field);
                if (psiClass == null) {
                    return;
                }

                if (!SideOnlyUtil.beginningCheck(field)) {
                    return;
                }

                Side fieldSide = SideOnlyUtil.checkField((PsiFieldImpl) field);
                if (fieldSide == Side.INVALID) {
                    return;
                }

                Side classSide = SideOnlyUtil.getSideForClass(psiClass);

                if (fieldSide != Side.NONE && fieldSide != classSide) {
                    if (classSide != Side.NONE && classSide != Side.INVALID) {
                        registerFieldError(field, fieldSide.getName(), classSide.getName(), field, true, false);
                    } else if (classSide != Side.NONE) {
                        registerFieldError(field, fieldSide.getName(), classSide.getName(), field, true, false);
                    }
                }

                if (!(field.getType() instanceof PsiClassType)) {
                    return;
                }

                PsiClassType type = (PsiClassType) field.getType();
                PsiClass fieldClass = type.resolve();
                if (fieldClass == null) {
                    return;
                }

                Side fieldClassSide = SideOnlyUtil.getSideForClass(fieldClass);

                if (fieldClassSide == Side.NONE || fieldClassSide == Side.INVALID) {
                    return;
                }

                if (fieldClassSide != fieldSide && fieldSide != Side.NONE) {
                    registerFieldError(field, fieldSide.getName(), fieldClassSide.getName(), field, false, false);
                }

                if (fieldClassSide != classSide) {
                    registerFieldError(field, fieldSide.getName(), fieldClassSide.getName(), field, false, true);
                }
            }
        };
    }
}
