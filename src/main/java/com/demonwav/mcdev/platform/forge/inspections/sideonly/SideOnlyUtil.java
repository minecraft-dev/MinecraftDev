package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.demonwav.mcdev.MinecraftSettings;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.forge.util.ForgeConstants;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.impl.source.PsiFieldImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public final class SideOnlyUtil {

    public static boolean beginningCheck(@NotNull PsiElement element) {
        // Don't check if this is disabled
        if (!MinecraftSettings.getInstance().isEnableSideOnlyChecks()) {
            return false;
        }

        // We need the module to get the MinecraftModule
        final Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return false;
        }

        // Check that the MinecraftModule
        //   1. Exists
        //   2. Is a ForgeModuleType
        final MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        return minecraftModule != null && minecraftModule.isOfType(ForgeModuleType.getInstance());
    }

    @NotNull
    public static String normalize(@NotNull String text) {
        if (text.startsWith(ForgeConstants.SIDE_ANNOTATION)) {
            // We chop off the "net.minecraftforge.fml.relauncher." part here
            return text.substring(text.lastIndexOf(".") - 4);
        }
        return text;
    }

    @NotNull
    public static Side checkMethod(@NotNull PsiMethod method) {
        final PsiAnnotation methodAnnotation = method.getModifierList().findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION);
        if (methodAnnotation == null) {
            // It's not annotated, which would be invalid if the element was annotated
            // (which, if we've gotten this far, is true)
            return Side.NONE;
        }

        // Check the value of the annotation
        final PsiAnnotationMemberValue methodValue = methodAnnotation.findAttributeValue("value");
        if (methodValue == null) {
            // The annotation has no value yet, IntelliJ will give it's own error because a value is required
            return Side.INVALID;
        }

        // Return the value of the annotation
        return getFromName(methodValue.getText());
    }

    @NotNull
    public static Side checkElementInMethod(@NotNull PsiElement element) {
        // Maybe there is a better way of doing this, I don't know, but crawl up the PsiElement stack in search of the
        // method this element is in. If it's not in a method it won't find one and the PsiMethod will be null
        PsiMethod method = null;
        while (method == null && element.getParent() != null) {
            final PsiElement parent = element.getParent();

            if (parent instanceof PsiMethod) {
                method = (PsiMethod) parent;
            } else {
                element = parent;
            }

            if (parent instanceof PsiClass) {
                break;
            }
        }

        // No method was found
        if (method == null) {
            return Side.INVALID;
        }

        return checkMethod(method);
    }

    @NotNull
    public static List<Pair<Side, PsiClass>> checkClassHierarchy(@NotNull PsiClass psiClass) {
        final List<PsiClass> classList = new LinkedList<>();
        classList.add(psiClass);

        PsiElement parent = psiClass;
        while (parent.getParent() != null) {
            parent = parent.getParent();

            if (parent instanceof PsiClass) {
                classList.add((PsiClass) parent);
            }
        }

        // We want to use an array list so indexing into the list is not expensive
        return classList.stream().map(SideOnlyUtil::checkClass).collect(Collectors.toCollection(ArrayList::new));
    }

    @NotNull
    public static Side getSideForClass(@NotNull PsiClass psiClass) {
        return getFirstSide(checkClassHierarchy(psiClass));
    }

    @NotNull
    public static Pair<Side, PsiClass> checkClass(@NotNull PsiClass psiClass) {
        final PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) {
            return new Pair<>(Side.NONE, psiClass);
        }

        // Check for the annotation, if it's not there then we return none, but this is
        // usually irrelevant for classes
        final PsiAnnotation annotation = modifierList.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION);
        if (annotation == null) {
            return new Pair<>(Side.NONE, psiClass);
        }

        // Check the value on the annotation. If it's not there, IntelliJ will throw
        // it's own error
        final PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
        if (value == null) {
            return new Pair<>(Side.INVALID, psiClass);
        }

        return new Pair<>(getFromName(value.getText()), psiClass);
    }

    @NotNull
    public static Side checkField(@NotNull PsiFieldImpl field) {
        // We check if this field has the @SideOnly annotation we are looking for
        // If it doesn't, we aren't worried about it
        final PsiAnnotation annotation = field.getModifierList().findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION);
        if (annotation == null) {
            return Side.NONE;
        }

        // The value may not necessarily be set, but that will give an error by default as "value" is a
        // required value for @SideOnly
        final PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
        if (value == null) {
            return Side.INVALID;
        }

        // Finally, get the value of the SideOnly
        return SideOnlyUtil.getFromName(value.getText());
    }

    @NotNull
    public static Side getFromName(@NotNull String name) {
        switch (normalize(name)) {
            case "Side.SERVER":
                return Side.SERVER;
            case "Side.CLIENT":
                return Side.CLIENT;
            default:
                return Side.INVALID;
        }
    }

    @NotNull
    public static Side getFirstSide(@NotNull List<Pair<Side, PsiClass>> list) {
        for (Pair<Side, PsiClass> pair : list) {
            if (pair.first != Side.NONE) {
                return pair.first;
            }
        }
        return Side.NONE;
    }

    @NotNull
    public static Object[] getSubArray(@NotNull Object[] infos) {
        final Object[] objects = new Object[2];
        System.arraycopy(infos, 1, objects, 0, 2);
        return objects;
    }
}
