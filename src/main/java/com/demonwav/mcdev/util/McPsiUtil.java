package com.demonwav.mcdev.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.google.common.collect.ImmutableSet;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiImmediateClassType;
import com.intellij.psi.impl.source.tree.java.PsiClassObjectAccessExpressionImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.java.IKeywordElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

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
                PsiReferenceList list = JavaPsiFacade.getElementFactory(project).createReferenceList(new PsiJavaCodeReferenceElement[] {element});
                psiClass.add(list);
            }
        }
    }

    @Nullable
    public static PsiClass resolveGenericClass(@Nullable PsiAnnotationMemberValue targetClasses) {
        if (targetClasses instanceof PsiArrayInitializerMemberValue) {
            final PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue) targetClasses).getInitializers();
            final PsiClassObjectAccessExpressionImpl targetExpression = ((PsiClassObjectAccessExpressionImpl) initializers[0]);
            final PsiType type = targetExpression.getType();

            if (!(type instanceof PsiImmediateClassType)) {
                return null;
            }
            final PsiSubstitutor substitutor = ((PsiImmediateClassType) type).resolveGenerics().getSubstitutor();
            final Map<PsiTypeParameter, PsiType> substitutionMap = substitutor.getSubstitutionMap();
            final Set<Map.Entry<PsiTypeParameter, PsiType>> entries = substitutionMap.entrySet();
            if (entries.size() != 1) {
                return null;
            }
            final Map.Entry<PsiTypeParameter, PsiType> next = entries.iterator().next();
            final PsiClassReferenceType value = (PsiClassReferenceType) next.getValue();
            return value.resolve();
        }
        if (targetClasses instanceof PsiClassObjectAccessExpressionImpl) {
            final PsiType type = ((PsiClassObjectAccessExpressionImpl) targetClasses).getType();
            if (!(type instanceof PsiImmediateClassType)) {
                return null;
            }
            final PsiSubstitutor substitutor = ((PsiImmediateClassType) type).resolveGenerics().getSubstitutor();
            final Map<PsiTypeParameter, PsiType> substitutionMap = substitutor.getSubstitutionMap();
            final Set<Map.Entry<PsiTypeParameter, PsiType>> entries = substitutionMap.entrySet();
            if (entries.size() != 1) {
                return null;
            }
            final Map.Entry<PsiTypeParameter, PsiType> next = entries.iterator().next();
            final PsiClassReferenceType value = (PsiClassReferenceType) next.getValue();
            return value.resolve();
        }
        return null;
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
}
