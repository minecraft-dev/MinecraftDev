/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.util.findAnnotation
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReferenceExpression
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

    fun checkMethod(method: PsiMethod): Pair<SideAnnotation?, Side> {
        return findSide(method) ?: (null to Side.NONE)
    }

    fun checkElementInMethod(element: PsiElement): Pair<SideAnnotation?, Side> {
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
            return (null to Side.INVALID)
        }

        return checkMethod(method)
    }

    fun checkClassHierarchy(psiClass: PsiClass): List<Triple<SideAnnotation?, Side, PsiClass>> {
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

    fun getSideForClass(psiClass: PsiClass): Pair<SideAnnotation?, Side> {
        return getFirstSide(checkClassHierarchy(psiClass))
    }

    private fun checkClass(psiClass: PsiClass): Triple<SideAnnotation?, Side, PsiClass> {
        val knownSide = psiClass.getUserData(Side.KEY)
        if (knownSide != null) {
            return Triple(null, knownSide, psiClass)
        }

        // Check for a sided annotation, if it's not there then we search super classes
        findSide(psiClass)?.let { (annotation, side) ->
            if (side != Side.INVALID && side != Side.NONE) {
                return Triple(annotation, side, psiClass)
            }
        }

        if (psiClass.supers.isEmpty()) {
            return Triple(null, Side.NONE, psiClass)
        }

        // check the classes this class extends
        return psiClass.supers.asSequence()
            // Prevent stack-overflow on cyclic dependencies
            .filter { psiClass != it }
            .map { checkClassHierarchy(it) }
            .firstOrNull { it.isNotEmpty() }
            ?.let {
                val (annotation, side, _) = it[0]
                Triple(annotation, side, psiClass)
            } ?: Triple(null, Side.NONE, psiClass)
    }

    fun checkField(field: PsiField): Pair<SideAnnotation?, Side> {
        // We check if this field has a sided annotation we are looking for
        // If it doesn't, we aren't worried about it
        return findSide(field) ?: (null to Side.NONE)
    }

    /**
     * @return a pair of the first sided annotation found on the given element with its side value.
     * `null` is returned if no known side annotation was found. Side might be [Side.INVALID] but never [Side.NONE].
     */
    private fun findSide(element: PsiModifierListOwner): Pair<SideAnnotation, Side>? {
        for (sideAnnotation in SideAnnotation.KNOWN_ANNOTATIONS) {
            val annotation = element.findAnnotation(sideAnnotation.annotationName)
                ?: continue

            val sideValue = annotation.findAttributeValue("value")
                ?: return sideAnnotation to Side.INVALID

            if (sideValue is PsiReferenceExpression) {
                val referenced = sideValue.resolve()
                if (referenced is PsiEnumConstant) {
                    val enumClass = referenced.containingClass
                    if (enumClass?.qualifiedName != sideAnnotation.enumName) {
                        continue
                    }
                    val side = when (referenced.name) {
                        sideAnnotation.clientValue -> Side.CLIENT
                        sideAnnotation.serverValue -> Side.SERVER
                        else -> continue
                    }
                    return sideAnnotation to side
                }
            }
        }
        return null
    }

    fun getFirstSide(list: List<Triple<SideAnnotation?, Side, PsiClass>>): Pair<SideAnnotation?, Side> {
        return list.firstOrNull { it.first != null && it.second != Side.NONE }
            ?.let { it.first to it.second }
            ?: (null to Side.NONE)
    }

    fun <T : Any?> getSubArray(infos: Array<T>): Array<T> {
        return infos.copyOfRange(1, infos.size - 1)
    }
}
