/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiLiteral
import com.intellij.psi.ResolveResult
import com.intellij.util.ArrayUtilRt

/**
 * Provides reference resolution for the `targets` attribute of the `@Mixin` annotation.
 *
 * Resolves fully qualified class name strings to their corresponding PsiClass for navigation (Ctrl+Click).
 */
object MixinTargetsReference : PolyReferenceResolver() {

    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(MIXIN, "targets")

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        val fqn = (context as? PsiLiteral)?.value as? String ?: return ResolveResult.EMPTY_ARRAY
        val psiClass = findQualifiedClass(context.project, fqn, context.resolveScope)
            ?: return ResolveResult.EMPTY_ARRAY
        return arrayOf(PsiElementResolveResult(psiClass))
    }

    override fun collectVariants(context: PsiElement): Array<Any> = ArrayUtilRt.EMPTY_OBJECT_ARRAY
}