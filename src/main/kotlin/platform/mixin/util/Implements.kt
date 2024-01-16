/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

fun PsiClass.findSoftImplements(): Map<String, PsiClass>? {
    val implements = modifierList?.findAnnotation(IMPLEMENTS) ?: return null
    val interfaces =
        (implements.findDeclaredAttributeValue(null) ?: return null).findAnnotations().ifEmpty { return null }

    val result = HashMap<String, PsiClass>()
    for (iface in interfaces) {
        val prefix = iface.findDeclaredAttributeValue("prefix")?.constantStringValue ?: continue
        val psiClass = iface.findDeclaredAttributeValue("iface")?.resolveClass() ?: continue
        result[prefix] = psiClass
    }

    return result
}

fun PsiMethod.findSoftImplements(): Map<String, PsiClass>? {
    val methodName = name
    if ('$' !in methodName) {
        return null
    }

    val containingClass = containingClass ?: return null
    return containingClass.findSoftImplements()
}

fun PsiMethod.isSoftImplementedMethod(): Boolean {
    val softImplements = this.findSoftImplements()
    if (softImplements.isNullOrEmpty()) {
        return false
    }

    val methodName = name
    return softImplements.any { (prefix, _) -> methodName.startsWith(prefix) }
}

fun PsiMethod.isSoftImplementMissingParent(): Boolean {
    return forEachSoftImplementedMethods(true) { return false }
}

inline fun PsiMethod.forEachSoftImplementedMethods(checkBases: Boolean, func: (PsiMethod) -> Unit): Boolean {
    val softImplements = this.findSoftImplements()
    if (softImplements.isNullOrEmpty()) {
        return false
    }

    val methodName = name

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
