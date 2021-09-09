/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.METHOD_INJECTORS
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral

object MethodReference : AbstractMethodReference() {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> =
        PsiJavaPatterns.psiLiteral(StandardPatterns.string()).insideAnnotationParam(
            StandardPatterns.string().oneOf(METHOD_INJECTORS),
            "method"
        )

    override val description = "method '%s' in target class"

    override fun isValidAnnotation(name: String) = name in METHOD_INJECTORS

    override fun parseSelector(context: PsiElement): MixinSelector? {
        return parseMixinSelector(context)
    }

    override fun parseSelector(stringValue: String, context: PsiElement): MixinSelector? {
        return parseMixinSelector(stringValue, context)
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        return if (super.isUnresolved(context)) {
            val stringValue = context.constantStringValue ?: return true
            !isMiscDynamicSelector(context.project, stringValue)
        } else {
            false
        }
    }
}
