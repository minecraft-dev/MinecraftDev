/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.inspection;

import com.demonwav.mcdev.platform.canary.util.CanaryConstants;
import com.demonwav.mcdev.util.McPsiUtil;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CanaryCommandImplementedInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Canary @Command in class not implementing CommandListener";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        return "This class contains @Command methods but does not implement CommandListener.";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "All Canary @Command methods must reside in a class that implements CommandListener.";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        return new InspectionGadgetsFix() {
            @Override
            protected void doFix(Project project, ProblemDescriptor descriptor) {
                PsiClass psiClass = (PsiClass) infos[0];
                McPsiUtil.addImplements(psiClass, CanaryConstants.COMMAND_LISTENER_CLASS, project);
            }

            @Nls
            @NotNull
            @Override
            public String getName() {
                return "Implement CommandListener";
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
            public void visitClass(PsiClass aClass) {
                final PsiMethod[] methods = aClass.getMethods();
                boolean isEventHandler = false;
                for (PsiMethod method : methods) {
                    PsiModifierList list = method.getModifierList();
                    PsiAnnotation annotation = list.findAnnotation(CanaryConstants.COMMAND_ANNOTATION);
                    if (annotation != null) {
                        isEventHandler = true;
                        break;
                    }
                }

                if (!isEventHandler) {
                    return;
                }

                final boolean inError = !McPsiUtil.extendsOrImplementsClass(aClass, CanaryConstants.COMMAND_LISTENER_CLASS);

                if (inError) {
                    registerClassError(aClass, aClass);
                }
            }
        };
    }
}
