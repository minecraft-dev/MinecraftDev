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

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.psi.PsiClass

class MixinCompletionWeigher : CompletionWeigher() {
    private val mixinAnnotation = PlatformPatterns.psiElement()
        .inside(
            false,
            PsiJavaPatterns.psiAnnotation().qName(MixinConstants.Annotations.MIXIN),
            PlatformPatterns.psiFile(),
        )!!

    override fun weigh(element: LookupElement, location: CompletionLocation): Int {
        val lookupClass = element.psiElement as? PsiClass ?: return 0

        val position = location.completionParameters.position
        if (!mixinAnnotation.accepts(position)) {
            return 0
        }

        val clazz = position.findContainingClass() ?: return 0
        if (clazz equivalentTo lookupClass) {
            return -1
        }

        return 0
    }
}
