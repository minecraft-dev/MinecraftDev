/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.toResolveResults
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtilRt
import org.objectweb.asm.Type

object InvokerReference : PolyReferenceResolver() {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(MixinConstants.Annotations.INVOKER)

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        val project = context.project
        return if (context.findContainingMethod() == null) {
            // Handle incomplete code that doesn't have a method yet. Just search for the method by name
            val name = (context as? PsiLiteral)?.value as? String ?: return ResolveResult.EMPTY_ARRAY
            if (name == "<clinit>") {
                return ResolveResult.EMPTY_ARRAY
            }
            val mixinClass = context.findContainingClass() ?: return ResolveResult.EMPTY_ARRAY
            mixinClass.mixinTargets.asSequence().flatMap { clazz ->
                clazz.methods?.filter { method -> method.name == name }?.mapNotNull { method ->
                    method.findSourceElement(clazz, project, context.resolveScope, canDecompile = false)
                } ?: emptyList()
            }.toResolveResults()
        } else {
            val annotation = context.parentOfType<PsiAnnotation>() ?: return ResolveResult.EMPTY_ARRAY
            MixinAnnotationHandler.resolveTarget(annotation).asSequence()
                .mapNotNull { it.findSourceElement(project, context.resolveScope, canDecompile = false) }
                .toResolveResults()
        }
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val project = context.project
        val mixinClass = context.findContainingClass() ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
        val method = context.findContainingMethod()
        if (method == null) {
            // Handle incomplete code by returning all methods
            return mixinClass.mixinTargets.asSequence().flatMap { clazz ->
                clazz.methods?.filter { method -> method.name != "<clinit>" }?.map { method ->
                    LookupElementBuilder.create(
                        method.findOrConstructSourceMethod(clazz, project, context.resolveScope, canDecompile = false),
                        method.name
                    )
                } ?: emptyList()
            }.toTypedArray()
        } else {
            val methodDesc = method.descriptor
            val constructorType = Type.getReturnType(methodDesc).takeIf { it.sort == Type.OBJECT }?.internalName
            val constructorDesc = constructorType?.let { methodDesc?.substringBeforeLast(')') + ")V" }
            return mixinClass.mixinTargets.asSequence().flatMap { clazz ->
                clazz.methods?.filter { method ->
                    method.name != "<clinit>" && if (method.isConstructor) {
                        clazz.name == constructorType && method.desc == constructorDesc
                    } else {
                        method.desc == methodDesc
                    }
                }?.map { method ->
                    LookupElementBuilder.create(
                        method.findOrConstructSourceMethod(clazz, project, context.resolveScope, canDecompile = false),
                        method.name
                    )
                } ?: emptyList()
            }.toTypedArray()
        }
    }
}
