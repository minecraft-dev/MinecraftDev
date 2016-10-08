/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.mixin.inspections;

import com.demonwav.mcdev.platform.mixin.util.MixinConstants;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.util.McPsiUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StaticMixinMemberInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Public static fields/non-Overwrite methods in a Mixin";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "A mixin class does not exist at runtime, and thus having them public does not make sense. Make the field/method private instead.";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        PsiMember member = (PsiMember) infos[0];

        if (member instanceof PsiField) {
            return this.getFieldMessage(member);
        } else if (member instanceof PsiMethod) {
            return this.getMethodMessage(member);
        }

        throw new IllegalArgumentException("Cannot build error message for PsiMember " + member);
    }

    private String getFieldMessage(PsiMember field) {
        return String.format("Static field '%s' is part of a Mixin, and therefore must not be public", field.getName());
    }

    private String getMethodMessage(PsiMember method) {
        return String.format("Non-Overwrite static method %s is part of a Mixin, and therefore must not be public", method.getName());
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {

            @Override
            public void visitMethod(PsiMethod method) {
                if (isProblematic(method)) {
                    this.registerMethodError(method, method);
                }
            }

            @Override
            public void visitField(PsiField field) {
                if (isProblematic(field)) {
                    this.registerError(field, field);
                }
            }

            private boolean isProblematic(PsiMember member) {
                PsiClass mixin = MixinUtils.getContainingMixinClass(member);
                if (mixin == null) {
                    return false;
                }

                PsiModifierList modifierList = mixin.getModifierList();

                if (modifierList == null) {
                    return false;
                }

                boolean isOverwrite = member instanceof PsiMethod && McPsiUtil.getAnnotation(member, MixinConstants.Annotations.OVERWRITE) != null;

                if (!isOverwrite && member.hasModifierProperty(PsiModifier.PUBLIC) && member.hasModifierProperty(PsiModifier.STATIC)) {
                    return true;
                }
                return false;
            }
        };
    }
}
