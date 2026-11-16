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

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReference
import com.intellij.psi.javadoc.CustomJavadocTagProvider
import com.intellij.psi.javadoc.JavadocTagInfo
import com.intellij.psi.javadoc.PsiDocTagValue

class MixinCustomJavaDocTagProvider : CustomJavadocTagProvider {

    override fun getSupportedTags(): List<JavadocTagInfo> = listOf(InjectorTag.Author, InjectorTag.Reason)

    private sealed class InjectorTag : JavadocTagInfo {

        override fun isInline() = false

        override fun isValidInContext(element: PsiElement?): Boolean {
            if (element !is PsiMember) {
                return false
            }

            return element.annotations.any { MixinAnnotationHandler.forMixinAnnotation(it) != null }
        }

        override fun checkTagValue(value: PsiDocTagValue?): String? = null
        override fun getReference(value: PsiDocTagValue?): PsiReference? = null

        object Author : InjectorTag() {

            override fun getName() = "author"

            override fun checkTagValue(value: PsiDocTagValue?): String? {
                return MCDevBundle("mixin.javadoc.author_required").takeIf { value?.text.isNullOrBlank() }
            }
        }

        object Reason : InjectorTag() {
            override fun getName() = "reason"

            override fun checkTagValue(value: PsiDocTagValue?): String? {
                return MCDevBundle("mixin.javadoc.reason_required").takeIf { value?.text.isNullOrBlank() }
            }
        }
    }
}
