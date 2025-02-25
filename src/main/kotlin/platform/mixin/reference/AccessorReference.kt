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
import com.demonwav.mcdev.platform.mixin.util.findFieldByName
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceField
import com.demonwav.mcdev.platform.mixin.util.findSourceField
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.toResolveResults
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiTypes
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtilRt

object AccessorReference : PolyReferenceResolver() {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(MixinConstants.Annotations.ACCESSOR)

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        val project = context.project
        return if (context.findContainingMethod() == null) {
            // Handle incomplete code that doesn't have a method yet. Just search for the field by name
            val name = (context as? PsiLiteral)?.value as? String ?: return ResolveResult.EMPTY_ARRAY
            val mixinClass = context.findContainingClass() ?: return ResolveResult.EMPTY_ARRAY
            mixinClass.mixinTargets.asSequence().mapNotNull {
                it.findFieldByName(name)?.findSourceField(it, project, context.resolveScope, canDecompile = false)
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

        val fieldDesc = context.findContainingMethod()?.let { method ->
            val type = method.returnType ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
            if (type == PsiTypes.voidType()) {
                method.parameterList.getParameter(0)?.type
            } else {
                type
            }
        }?.descriptor

        return mixinClass.mixinTargets.asSequence().flatMap { clazz ->
            clazz.fields?.filter { field -> fieldDesc == null || field.desc == fieldDesc }?.map { field ->
                field.findOrConstructSourceField(clazz, project, context.resolveScope, canDecompile = false)
            } ?: emptyList()
        }.toTypedArray()
    }
}
