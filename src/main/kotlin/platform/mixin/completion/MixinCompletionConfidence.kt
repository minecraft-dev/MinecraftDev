/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.SkipAutopopupInStrings
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

class MixinCompletionConfidence : CompletionConfidence() {

    private val mixinAnnotation = PlatformPatterns.psiElement()
        .inside(
            false,
            PsiJavaPatterns.psiAnnotation().qName(
                StandardPatterns.or(
                    StandardPatterns.string().startsWith(MixinConstants.PACKAGE),
                    StandardPatterns.string()
                        .oneOf(MixinAnnotationHandler.getBuiltinHandlers().map { it.first }.toList()),
                )
            ),
            PlatformPatterns.psiFile(),
        )!!

    override fun shouldSkipAutopopup(element: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        // Enable auto complete for all string literals which are children of one of the annotations in Mixin
        // TODO: Make this more reliable (we don't need to enable it for all parts of the annotation)
        return if (SkipAutopopupInStrings.isInStringLiteral(element) && mixinAnnotation.accepts(element)) {
            ThreeState.NO
        } else {
            ThreeState.UNSURE
        }
    }
}
