/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.IMPLEMENTS
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findMatchingMethods
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.resolveClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import org.jetbrains.annotations.Contract
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@Contract(pure = true)
fun PsiClass.findSoftImplements(): Map<String, PsiClass>? {
    val implements = modifierList?.findAnnotation(IMPLEMENTS) ?: return null
    val interfaces = (implements.findDeclaredAttributeValue(null) ?: return null).findAnnotations().ifEmpty { return null }

    val result = HashMap<String, PsiClass>()
    for (iface in interfaces) {
        val prefix = iface.findDeclaredAttributeValue("prefix")?.constantStringValue ?: continue
        val psiClass = iface.findDeclaredAttributeValue("iface")?.resolveClass() ?: continue
        result[prefix] = psiClass
    }

    return result
}

@Contract(pure = true)
fun PsiMethod.isSoftImplementedMethod(): Boolean {
    val methodName = name
    if ('$' !in methodName) {
        return false
    }

    val containingClass = containingClass ?: return false
    val softImplements = containingClass.findSoftImplements() ?: return false
    if (softImplements.isEmpty()) {
        return false
    }

    return softImplements.any { (prefix, _) -> methodName.startsWith(prefix) }
}

@Contract(pure = true)
fun PsiMethod.isSoftImplementMissingParent(): Boolean {
    return findSoftImplementedMethods(true) { return false }
}

@Contract(pure = true)
inline fun PsiMethod.findSoftImplementedMethods(checkBases: Boolean, func: (PsiMethod) -> Unit): Boolean {
    val methodName = name
    if ('$' !in methodName) {
        return false
    }

    val containingClass = containingClass ?: return false
    val softImplements = containingClass.findSoftImplements() ?: return false
    if (softImplements.isEmpty()) {
        return false
    }

    var foundPrefix = false

    for ((prefix, iface) in softImplements) {
        if (!methodName.startsWith(prefix)) {
            continue
        }

        foundPrefix = true
        iface.findMatchingMethods(this, checkBases, methodName.removePrefix(prefix)) { superMethod ->
            if (!superMethod.hasModifierProperty(PsiModifier.STATIC)) {
                func(superMethod)
            }
        }
    }

    return foundPrefix
}
