/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.reference

import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.reference.ReferenceResolver
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression

object GetterEventListenerReferenceResolver : ReferenceResolver() {

    override fun resolveReference(context: PsiElement): PsiElement? {
        val method = context.findContainingMethod() ?: return null
        if (!method.hasParameters()) {
            return null
        }

        val eventType = method.parameters[0].type as? JvmReferenceType ?: return null

        val methodName = (context as PsiLiteralExpression).value.toString()
        val methods = (eventType.resolve() as? PsiClass)?.findMethodsByName(methodName, true) ?: return null
        return methods.firstOrNull { !it.hasParameters() }
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        return emptyArray()
    }
}
