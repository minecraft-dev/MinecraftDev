package com.demonwav.mcdev.platform.mixin.inspections;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MixinInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid Mixin";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        final PsiClass erroneousClass = (PsiClass) infos[0];
        final String name = erroneousClass.getName();
        return name + " is not targeting anything to mixin!";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "A Mixin class must target either one or more classes or provide one or more string targets";
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitClass(PsiClass aClass) {
                final PsiModifierList modifierList = aClass.getModifierList();
                if (modifierList == null) {
                    return;
                }
                final PsiAnnotation mixin = modifierList.findAnnotation("org.spongepowered.asm.mixin.Mixin");
                if (mixin == null) {
                    return;
                }
                final PsiAnnotationMemberValue classValues = mixin.findDeclaredAttributeValue("value");
                final PsiAnnotationMemberValue stringTargets = mixin.findDeclaredAttributeValue("targets");
                if (classValues == null && stringTargets == null) {
                    registerError(mixin, aClass);
                }
            }
        };
    }
}
