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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.util.annotationFromNameValuePair
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameValuePair

abstract class MixinAnnotationAttributeInspection(
    private val annotation: String?,
    private val attribute: String?,
) : MixinInspection() {

    constructor(attribute: String?) : this(null, attribute)

    protected abstract fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder,
    )

    final override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private inner class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitNameValuePair(pair: PsiNameValuePair) {
            if (
                pair.name != attribute &&
                (attribute != null || pair.name != PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME)
            ) {
                return
            }

            val psiAnnotation = pair.annotationFromNameValuePair ?: return

            if (annotation != null && !psiAnnotation.hasQualifiedName(annotation)) {
                return
            }

            val value = pair.value ?: return
            visitAnnotationAttribute(psiAnnotation, value, this.holder)
        }
    }
}
