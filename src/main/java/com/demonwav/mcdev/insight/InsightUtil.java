/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class InsightUtil {

    @Nullable
    public static Pair<PsiClass, PsiMethod> getEventListenerFromElement(@NotNull PsiElement element) {
        // Since we want to line up with the method declaration, not the annotation
        // declaration, we need to target identifiers, not just PsiMethods.
        if (!(element instanceof PsiIdentifier && (element.getParent() instanceof PsiMethod))) {
            return null;
        }
        // The PsiIdentifier is going to be a method of course!
        PsiMethod method = (PsiMethod) element.getParent();
        if (method.hasModifierProperty(PsiModifier.ABSTRACT) ||
            method.hasModifierProperty(PsiModifier.PRIVATE)) {
            // I don't think any implementation allows for abstract method listeners.
            return null;
        }
        PsiModifierList modifierList = method.getModifierList();
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return null;
        }
        MinecraftModule instance = MinecraftModule.getInstance(module);
        if (instance == null) {
            return null;
        }
        // Since each platform has their own valid listener annotations,
        // some platforms may have multiple allowed annotations for various cases
        final List<String> listenerAnnotations = instance.getTypes().stream()
            .flatMap(t -> t.getListenerAnnotations().stream()).collect(Collectors.toList());
        boolean contains = false;
        for (String listenerAnnotation : listenerAnnotations) {
            if (modifierList.findAnnotation(listenerAnnotation) != null) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            return null;
        }
        final PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length < 1) {
            return null;
        }
        final PsiParameter psiParameter = parameters[0];
        if (psiParameter == null) {
            // Listeners must have at least a single parameter
            return null;
        }
        // Get the type of the parameter so we can start resolving it
        PsiTypeElement psiEventElement = psiParameter.getTypeElement();
        if (psiEventElement == null) {
            return null;
        }
        final PsiType type = psiEventElement.getType();
        // Validate that it is a class reference type, I don't know if this will work with
        // other JVM languages such as Kotlin or Scala, but it might!
        if (!(type instanceof PsiClassReferenceType)) {
            return null;
        }
        // And again, make sure that we can at least resolve the type, otherwise it's not a valid
        // class reference.
        final PsiClass resolve = ((PsiClassReferenceType) type).resolve();
        if (resolve == null) {
            return null;
        }

        if (!instance.isStaticListenerSupported(resolve, method) && method.hasModifierProperty(PsiModifier.STATIC)) {
            return null;
        }

        return Pair.create(resolve, method);
    }

    @Nullable
    public static Pair<PsiParameter, PsiClass> getEventParameterPairFromMethod(PsiMethod method) {
        final PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length < 1) {
            return null;
        }
        final PsiParameter psiParameter = parameters[0];
        if (psiParameter == null) {
            // Listeners must have at least a single parameter
            return null;
        }
        // Get the type of the parameter so we can start resolving it
        PsiTypeElement psiEventElement = psiParameter.getTypeElement();
        if (psiEventElement == null) {
            return null;
        }
        final PsiType type = psiEventElement.getType();
        // Validate that it is a class reference type, I don't know if this will work with
        // other JVM languages such as Kotlin or Scala, but it might!
        if (!(type instanceof PsiClassReferenceType)) {
            return null;
        }
        // And again, make sure that we can at least resolve the type, otherwise it's not a valid
        // class reference.
        final PsiClass resolve = ((PsiClassReferenceType) type).resolve();
        if (resolve == null) {
            return null;
        }
        return Pair.create(psiParameter, resolve);
    }
}
