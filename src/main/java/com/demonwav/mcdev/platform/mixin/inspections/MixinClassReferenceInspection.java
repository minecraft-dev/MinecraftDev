package com.demonwav.mcdev.platform.mixin.inspections;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MixinClassReferenceInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Reference to Mixin class!";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        PsiClass mixinClass = (PsiClass) infos[0];
        return mixinClass.getName() + " is a Mixin class, and cannot be referenced directly!";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "A Mixin class doesn't exist at runtime, and thus cannot be referenced directly.";
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {

            @Override
            public void visitTypeElement(PsiTypeElement typeElement) {
                final PsiType type = typeElement.getType();
                if (!(type instanceof PsiClassReferenceType)) {
                    return;
                }

                final PsiClass clazz = ((PsiClassReferenceType) type).resolve();
                if (clazz == null) {
                    return;
                }

                final PsiModifierList modifierList = clazz.getModifierList();
                if (modifierList == null) {
                    return;
                }
                final PsiAnnotation mixin = modifierList.findAnnotation("org.spongepowered.asm.mixin.Mixin");
                if (mixin == null) {
                    return;
                }

                registerError(typeElement, clazz);
            }
        };
    }
}
