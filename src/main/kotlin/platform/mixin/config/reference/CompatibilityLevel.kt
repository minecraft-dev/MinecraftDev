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

package com.demonwav.mcdev.platform.mixin.config.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.COMPATIBILITY_LEVEL
import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext

object CompatibilityLevel : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
        arrayOf(Reference(element))

    private class Reference(element: PsiElement) : PsiReferenceBase<PsiElement>(element), InspectionReference {

        override val description
            get() = "compatibility level '%s'"

        override val unresolved
            get() = resolve() == null

        override fun resolve(): PsiElement? {
            val compatibilityLevel = findCompatibilityLevel(element)
            return compatibilityLevel?.findFieldByName(value, false)?.takeIf { it is PsiEnumConstant }
        }

        override fun getVariants(): Array<Any> {
            val compatibilityLevel = findCompatibilityLevel(element) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
            val list = ArrayList<LookupElementBuilder>()
            for (field in compatibilityLevel.fields) {
                if (field !is PsiEnumConstant) {
                    continue
                }

                list.add(LookupElementBuilder.create(field.name))
            }

            return list.toArray()
        }

        override fun isReferenceTo(element: PsiElement) = element is PsiEnumConstant && super.isReferenceTo(element)

        private fun findCompatibilityLevel(context: PsiElement) =
            JavaPsiFacade.getInstance(context.project).findClass(COMPATIBILITY_LEVEL, context.resolveScope)
    }
}
