/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("McMethodUtil")
package com.demonwav.mcdev.util

import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.util.MethodSignature
import org.jetbrains.annotations.Contract

@Contract(pure = true)
fun areSignaturesEqualLightweight(sig1: MethodSignature,
                                  sig2: MethodSignature,
                                  sig2NameReplacement: String): Boolean {
    val isConstructor1 = sig1.isConstructor
    val isConstructor2 = sig2.isConstructor
    if (isConstructor1 != isConstructor2) {
        return false
    }

    if (!isConstructor1 || !(sig1 is HierarchicalMethodSignature || sig2 is HierarchicalMethodSignature)) {
        val name1 = sig1.name
        if (name1 != sig2NameReplacement) {
            return false
        }
    }

    val parameterTypes1 = sig1.parameterTypes
    val parameterTypes2 = sig2.parameterTypes
    if (parameterTypes1.size != parameterTypes2.size) return false

    // optimization: check for really different types in method parameters
    for (i in parameterTypes1.indices) {
        val type1 = parameterTypes1[i]
        val type2 = parameterTypes2[i]
        if (type1 is PsiPrimitiveType != type2 is PsiPrimitiveType) {
            return false
        }
        if (type1 is PsiPrimitiveType && type1 != type2) {
            return false
        }
    }

    return true
}

@Contract(value = "null -> null", pure = true)
fun getContainingMethod(element: PsiElement?): PsiMethod? {
    if (element == null) {
        return null
    }

    if (element is PsiMethod) {
        return element
    }

    // Class, File, and Directory, if we've hit that, we're not in a method
    // Member, if we've hit that immediately after failing the "are we a method" check, then we
    // aren't in a method
    if (element is PsiClass || element is PsiMember || element is PsiFile || element is PsiDirectory) {
        return null
    }

    return getContainingMethod(element.parent ?: return null)
}
