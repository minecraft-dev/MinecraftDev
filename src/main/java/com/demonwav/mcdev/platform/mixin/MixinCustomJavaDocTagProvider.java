package com.demonwav.mcdev.platform.mixin;

import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.mixin.inspections.AuthorInspection;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.javadoc.CustomJavadocTagProvider;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocTagValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MixinCustomJavaDocTagProvider implements CustomJavadocTagProvider {

    @Override
    public List<JavadocTagInfo> getSupportedTags() {
        return ImmutableList.of(new JavadocTagInfo() {
            @Override
            public String getName() {
                return "author";
            }

            @Override
            public boolean isInline() {
                return false;
            }

            @Override
            public boolean isValidInContext(PsiElement element) {
                return MixinCustomJavaDocTagProvider.isValidInContext(element);
            }

            @Nullable
            @Override
            public String checkTagValue(PsiDocTagValue value) {
                if (value == null || Strings.isNullOrEmpty(value.getText().trim())) {
                    return "The @author JavaDoc tag on @Overwrite methods must be filled in.";
                }
                return null;
            }

            @Nullable
            @Override
            public PsiReference getReference(PsiDocTagValue value) {
                return null;
            }
        }, new JavadocTagInfo() {
            @Override
            public String getName() {
                return "reason";
            }

            @Override
            public boolean isInline() {
                return false;
            }

            @Override
            public boolean isValidInContext(PsiElement element) {
                return MixinCustomJavaDocTagProvider.isValidInContext(element);
            }

            @Nullable
            @Override
            public String checkTagValue(PsiDocTagValue value) {
                return null;
            }

            @Nullable
            @Override
            public PsiReference getReference(PsiDocTagValue value) {
                return null;
            }
        });
    }

    private static boolean isValidInContext(@NotNull PsiElement element) {
        if (!(element instanceof PsiMethod)) {
            return false;
        }

        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return false;
        }

        MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule == null) {
            return false;
        }

        if (!minecraftModule.isOfType(MixinModuleType.getInstance())) {
            return false;
        }

        PsiMethod method = (PsiMethod) element;

        return AuthorInspection.shouldHaveAuthorTag(method);
    }
}
