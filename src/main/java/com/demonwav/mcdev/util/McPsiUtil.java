/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.navigation.AnonymousElementProvider;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class McPsiUtil {
    private McPsiUtil() {}

    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiClass getClassOfElement(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        if (element instanceof PsiClass) {
            return (PsiClass) element;
        }

        while (element.getParent() != null) {

            if (element.getParent() instanceof PsiClass) {
                return (PsiClass) element.getParent();
            }

            if (element.getParent() instanceof PsiFile || element.getParent() instanceof PsiDirectory) {
                return null;
            }

            element = element.getParent();
        }
        return null;
    }

    public static boolean extendsOrImplementsClass(@NotNull PsiClass psiClass, @NotNull String qualifiedClassName) {
        final PsiClass[] supers = psiClass.getSupers();
        for (PsiClass aSuper : supers) {
            if (qualifiedClassName.equals(aSuper.getQualifiedName())) {
                return true;
            }
            if (extendsOrImplementsClass(aSuper, qualifiedClassName)) {
                return true;
            }
        }
        return false;
    }

    public static void addImplements(@NotNull PsiClass psiClass, @NotNull String qualifiedClassName, @NotNull Project project) {
        PsiReferenceList referenceList = psiClass.getImplementsList();
        PsiClass listenerClass = JavaPsiFacade.getInstance(project).findClass(qualifiedClassName, GlobalSearchScope.allScope(project));
        if (listenerClass != null) {
            PsiJavaCodeReferenceElement element = JavaPsiFacade.getElementFactory(project).createClassReferenceElement(listenerClass);
            if (referenceList != null) {
                referenceList.add(element);
            } else {
                PsiReferenceList list = JavaPsiFacade.getElementFactory(project).createReferenceList(new PsiJavaCodeReferenceElement[] {element});
                psiClass.add(list);
            }
        }
    }

    private static final ImmutableSet<String> METHOD_ACCESS_MODIFIERS = ImmutableSet.<String>builder()
            .add(PsiModifier.PUBLIC)
            .add(PsiModifier.PROTECTED)
            .add(PsiModifier.PACKAGE_LOCAL)
            .add(PsiModifier.PRIVATE)
            .build();

    public static String getAccessModifier(PsiMember member) {
        return METHOD_ACCESS_MODIFIERS.stream()
                .filter(member::hasModifierProperty)
                .findFirst()
                .orElse(PsiModifier.PUBLIC);
    }

    public static IElementType getMethodAccessType(PsiMethod method) {
        for (PsiElement modifier : method.getModifierList().getChildren()) {
            if (modifier instanceof PsiKeyword) {
                final IElementType tokenType = ((PsiKeyword) modifier).getTokenType();

            }
        }
        return JavaTokenType.PUBLIC_KEYWORD;
    }

    @Nullable
    public static PsiAnnotation getAnnotation(@Nullable PsiModifierListOwner owner, @NotNull String annotationName) {
        if (owner == null) {
            return null;
        }

        final PsiModifierList list = owner.getModifierList();
        if (list == null) {
            return null;
        }

        return list.findAnnotation(annotationName);
    }

    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static Pair<String, PsiClass> getNameOfClass(@Nullable PsiClass psiClass) {
        if (psiClass == null) {
            return null;
        }

        if (psiClass.getContainingClass() == null) {
            //noinspection ConstantConditions
            return Pair.create("", psiClass);
        }

        final List<String> innerStrings = Lists.newArrayList();
        PsiClass baseClass = psiClass;
        while (psiClass != null) {
            baseClass = psiClass;
            if (psiClass.getName() == null) {
                // anon class
                PsiElement[] anonymousClasses = null;
                for (AnonymousElementProvider provider : Extensions.getExtensions(AnonymousElementProvider.EP_NAME)) {
                    //noinspection ConstantConditions
                    anonymousClasses = provider.getAnonymousElements(psiClass.getContainingClass());
                    if (anonymousClasses.length > 0) {
                        break;
                    }
                }

                if (anonymousClasses == null) {
                    // We couldn't build the proper string, so don't return anything at all
                    return null;
                }

                for (int i = 0; i < anonymousClasses.length; i++) {
                    if (anonymousClasses[i] == psiClass) {
                        innerStrings.add(String.valueOf(i + 1));
                        break;
                    }
                }
            } else {
                innerStrings.add(psiClass.getName());
                psiClass = psiClass.getContainingClass();
            }
        }

        // We started from the bottom and went up, so reverse it
        Collections.reverse(innerStrings);
        // Skip the base class, we are giving the base PsiClass so the user can do with it what they want
        return Pair.create("$" + innerStrings.stream().skip(1).collect(Collectors.joining("$")), baseClass);
    }
}
