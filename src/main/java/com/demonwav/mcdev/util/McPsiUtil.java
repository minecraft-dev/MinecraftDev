package com.demonwav.mcdev.util;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiKeyword;
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

    public static String getMethodAccessModifier(PsiMethod method) {
        return METHOD_ACCESS_MODIFIERS.stream()
                .filter(method::hasModifierProperty)
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
}
