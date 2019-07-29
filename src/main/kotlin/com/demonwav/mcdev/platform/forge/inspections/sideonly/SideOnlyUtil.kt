/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.util.Arrays
import java.util.LinkedList

object SideOnlyUtil {

    fun beginningCheck(element: PsiElement): Boolean {
        // We need the module to get the MinecraftModule
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false

        // Check that the MinecraftModule
        //   1. Exists
        //   2. Is a ForgeModuleType
        val facet = MinecraftFacet.getInstance(module)
        return facet != null && facet.isOfType(ForgeModuleType)
    }

    private fun normalize(text: String): String {
        if (text.startsWith(ForgeConstants.SIDE_ANNOTATION)) {
            // We chop off the "net.minecraftforge.fml.relauncher." part here
            return text.substring(text.lastIndexOf(".") - 4)
        }
        return text
    }

    fun checkMethod(method: PsiMethod): Side {
        val methodAnnotation =
            // It's not annotated, which would be invalid if the element was annotated
            method.modifierList.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION)
            // (which, if we've gotten this far, is true)
                ?: return Side.NONE

        // Check the value of the annotation
        val methodValue =
            // The annotation has no value yet, IntelliJ will give it's own error because a value is required
            methodAnnotation.findAttributeValue("value")
                ?: return Side.INVALID

        // Return the value of the annotation
        return getFromName(methodValue.text)
    }

    fun checkElementInMethod(element: PsiElement): Side {
        var changingElement = element
        // Maybe there is a better way of doing this, I don't know, but crawl up the PsiElement stack in search of the
        // method this element is in. If it's not in a method it won't find one and the PsiMethod will be null
        var method: PsiMethod? = null
        while (method == null && changingElement.parent != null) {
            val parent = changingElement.parent

            if (parent is PsiMethod) {
                method = parent
            } else {
                changingElement = parent
            }

            if (parent is PsiClass) {
                break
            }
        }

        // No method was found
        if (method == null) {
            return Side.INVALID
        }

        return checkMethod(method)
    }

    fun checkClassHierarchy(psiClass: PsiClass): List<Pair<Side, PsiClass>> {
        val classList = LinkedList<PsiClass>()
        classList.add(psiClass)

        var parent: PsiElement = psiClass
        while (parent.parent != null) {
            parent = parent.parent

            if (parent is PsiClass) {
                classList.add(parent)
            }
        }

        // We want to use an array list so indexing into the list is not expensive
        return classList.map { checkClass(it) }
    }

    fun getSideForClass(psiClass: PsiClass): Side {
        return getFirstSide(checkClassHierarchy(psiClass))
    }

    private fun checkClass(psiClass: PsiClass): Pair<Side, PsiClass> {
        val side = psiClass.getUserData(Side.KEY)
        if (side != null) {
            return Pair(side, psiClass)
        }

        val modifierList = psiClass.modifierList ?: return Pair(Side.NONE, psiClass)

        // Check for the annotation, if it's not there then we return none, but this is
        // usually irrelevant for classes
        val annotation = modifierList.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION)
        if (annotation == null) {
            if (psiClass.supers.isEmpty()) {
                return Pair(Side.NONE, psiClass)
            }

            // check the classes this class extends
            return psiClass.supers.asSequence()
                .filter {
                    // Prevent stack-overflow on cyclic dependencies
                    psiClass != it
                }
                .map { checkClassHierarchy(it) }
                .firstOrNull { it.isNotEmpty() }?.let { Pair(it[0].getFirst(), psiClass) } ?: Pair(Side.NONE, psiClass)
        }

        // Check the value on the annotation. If it's not there, IntelliJ will throw
        // it's own error
        val value = annotation.findAttributeValue("value") ?: return Pair(Side.INVALID, psiClass)

        return Pair(getFromName(value.text), psiClass)
    }

    fun checkField(field: PsiField): Side {
        // We check if this field has the @SideOnly annotation we are looking for
        // If it doesn't, we aren't worried about it
        val modifierList = field.modifierList ?: return Side.NONE
        val annotation = modifierList.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION) ?: return Side.NONE

        // The value may not necessarily be set, but that will give an error by default as "value" is a
        // required value for @SideOnly
        val value = annotation.findAttributeValue("value") ?: return Side.INVALID

        // Finally, get the value of the SideOnly
        return SideOnlyUtil.getFromName(value.text)
    }

    private fun getFromName(name: String): Side {
        return when (normalize(name)) {
            "Side.SERVER" -> Side.SERVER
            "Side.CLIENT" -> Side.CLIENT
            else -> Side.INVALID
        }
    }

    fun getFirstSide(list: List<Pair<Side, PsiClass>>): Side {
        return list.firstOrNull { it.first !== Side.NONE }?.first ?: Side.NONE
    }

    fun <T : Any?> getSubArray(infos: Array<T>): Array<T> {
        return Arrays.copyOfRange(infos, 1, infos.size - 1)
    }
}
