/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.util.ProcessingContext

object MethodReference : AbstractMethodReference() {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> =
        PsiJavaPatterns.psiLiteral(StandardPatterns.string()).insideAnnotationAttribute(
            PsiJavaPatterns.psiAnnotation().with(
                object : PatternCondition<PsiAnnotation>("injector") {
                    override fun accepts(t: PsiAnnotation, context: ProcessingContext?): Boolean {
                        val qName = t.qualifiedName ?: return false
                        return MixinAnnotationHandler.forMixinAnnotation(qName, t.project) is InjectorAnnotationHandler
                    }
                }
            ),
            "method"
        )

    override val description = "method '%s' in target class"

    override fun isValidAnnotation(name: String, project: Project) =
        MixinAnnotationHandler.forMixinAnnotation(name, project) is InjectorAnnotationHandler

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
