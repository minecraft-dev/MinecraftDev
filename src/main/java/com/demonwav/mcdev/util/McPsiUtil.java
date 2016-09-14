package com.demonwav.mcdev.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class McPsiUtil {

    @Nullable
    public static PsiClass getClassOfElement(@NotNull PsiElement element) {
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
                PsiReferenceList list = JavaPsiFacade.getElementFactory(project).createReferenceList(new PsiJavaCodeReferenceElement[]{element});
                psiClass.add(list);
            }
        }
    }
}
