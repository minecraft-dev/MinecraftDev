package com.demonwav.mcdev.platform.mixin.inspections;

import com.demonwav.mcdev.platform.mixin.util.MixinConstants;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;

import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReferenceExpression;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FinalInspection extends BaseInspection {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "@Final inspection";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "@Final annotated fields cannot be modified, as the field it is targeting is final. This can be overridden with @Mutable.";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        return infos[0] + " cannot be modified, as it is annotated with @Final";
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitAssignmentExpression(PsiAssignmentExpression expression) { final PsiExpression lExpression = expression.getLExpression();

                if (!(lExpression instanceof PsiReferenceExpression)) {
                    return;
                }

                if (!MixinUtils.isMixinModule(expression)) {
                    return;
                }

                if (MixinUtils.getContainingMixinClass(expression) == null) {
                    return;
                }

                final PsiReferenceExpression referenceExpression = (PsiReferenceExpression) lExpression;

                final PsiElement resolve = referenceExpression.resolve();
                if (resolve == null) {
                    return;
                }

                if (!(resolve instanceof PsiModifierListOwner)) {
                    return;
                }

                final PsiModifierListOwner modifierListOwner = (PsiModifierListOwner) resolve;
                final PsiModifierList modifierList = modifierListOwner.getModifierList();
                if (modifierList == null) {
                    return;
                }

                if (modifierList.findAnnotation(MixinConstants.Annotations.FINAL) == null) {
                    return;
                }

                if (modifierList.findAnnotation(MixinConstants.Annotations.MUTABLE) != null) {
                    return;
                }

                registerError(expression, referenceExpression.getReferenceName());
            }
        };
    }
}
