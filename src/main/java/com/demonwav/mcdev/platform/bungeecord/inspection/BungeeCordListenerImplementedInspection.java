package com.demonwav.mcdev.platform.bungeecord.inspection;

import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants;
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

public class BungeeCordListenerImplementedInspection extends BaseInspection {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "BungeeCord EventHandler in class not implementing Listener";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        return "This class contains EventHandler methods but does not implement Listener";
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "All BungeeCord EventHandler methods must reside in a class that implements Listener.";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        return new InspectionGadgetsFix() {
            @Override
            protected void doFix(Project project, ProblemDescriptor descriptor) {
                PsiClass psiClass = (PsiClass) infos[0];
                McPsiUtil.addImplements(psiClass, BungeeCordConstants.BUNGEECORD_LISTENER_CLASS, project);
            }

            @Nls
            @NotNull
            @Override
            public String getName() {
                return "Implement Listener";
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
                PsiMethod[] methods = aClass.getMethods();
                boolean isEventHandler = false;
                for (PsiMethod method : methods) {
                    PsiModifierList list = method.getModifierList();
                    PsiAnnotation annotation = list.findAnnotation(BungeeCordConstants.BUNGEECORD_HANDLER_ANNOTATION);
                    if (annotation != null) {
                        isEventHandler = true;
                        break;
                    }
                }

                if (!isEventHandler) {
                    return;
                }

                boolean inError = !McPsiUtil.extendsOrImplementsClass(aClass, BungeeCordConstants.BUNGEECORD_LISTENER_CLASS);

                if (inError) {
                    registerClassError(aClass, aClass);
                }
            }
        };
    }
}
