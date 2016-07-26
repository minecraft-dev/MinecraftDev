package com.demonwav.mcdev.platform.mixin.inspections;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.javadoc.JavadocManager;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AuthorInspection extends BaseInspection {

    @NotNull
    @Override
    public String getShortName() {
        return "OverwriteAuthorRequired";
    }

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Overwrite lacking @author JavaDoc tag";
    }

    @NotNull
    @Override
    protected String buildErrorString(Object... infos) {
        boolean authorTagExists = (boolean) infos[0];

        if (authorTagExists) {
            return "The @author JavaDoc tag on @Overwrite methods must be filled in.";
        } else {
            return "@Overwrite methods must have an associated JavaDoc with a filled in @author tag";
        }
    }

    @Nullable
    @Override
    public String getStaticDescription() {
        return "For maintainability reasons, the Sponge project requires @Overwrite methods to declared an @author JavaDoc tag.";
    }

    @Nullable
    @Override
    protected InspectionGadgetsFix buildFix(Object... infos) {
        if (!((boolean) infos[0])) {
            return new InspectionGadgetsFix() {
                @Override
                protected void doFix(Project project, ProblemDescriptor descriptor) {
                    PsiMethod method = (PsiMethod) infos[1];

                    PsiDocComment docComment = method.getDocComment();
                    if (docComment == null) {
                        PsiDocComment comment = JavaPsiFacade.getElementFactory(method.getProject())
                            .createDocCommentFromText("/**\n * @author \n */");
                        method.addBefore(comment, method.getModifierList());
                        return;
                    }

                    PsiDocTag tag = JavaPsiFacade.getElementFactory(method.getProject()).createDocTagFromText("@author");

                    if (docComment.getTags().length != 0) {
                        docComment.addAfter(tag, docComment.getTags()[docComment.getTags().length - 1]);
                    } else {
                        docComment.add(tag);
                    }
                }

                @Nls
                @NotNull
                @Override
                public String getName() {
                    return "Add @author tag JavaDoc";
                }

                @Nls
                @NotNull
                @Override
                public String getFamilyName() {
                    return getName();
                }
            };
        }
        return null;
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new BaseInspectionVisitor() {
            @Override
            public void visitMethod(PsiMethod method) {
                if (!shouldHaveAuthorTag(method)) {
                    return;
                }

                Module module = ModuleUtilCore.findModuleForPsiElement(method);
                if (module == null) {
                    return;
                }

                MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
                if (minecraftModule == null) {
                    return;
                }

                if (!minecraftModule.isOfType(MixinModuleType.getInstance())) {
                    return;
                }

                PsiDocComment docComment = method.getDocComment();
                if (docComment == null) {
                    registerMethodError(method, false, method);
                    return;
                }

                PsiDocTag[] tags = docComment.getTags();
                for (PsiDocTag tag : tags) {
                    String name = tag.getName();
                    if (!Objects.equals(name, "author")) {
                        continue;
                    }

                    return;
                }

                registerMethodError(method, false, method);
            }
        };
    }

    public static boolean shouldHaveAuthorTag(@NotNull PsiMethod method) {
        PsiModifierList list = method.getModifierList();

        PsiAnnotation annotation = list.findAnnotation("org.spongepowered.asm.mixin.Overwrite");
        return annotation != null;
    }
}
