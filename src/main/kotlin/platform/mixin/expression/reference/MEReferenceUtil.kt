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

package com.demonwav.mcdev.platform.mixin.expression.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.parentOfType

object MEReferenceUtil {
    fun isDefinitionId(element: PsiElement): Boolean {
        val parent = element.parent
        return parent is PsiNameValuePair &&
            parent.name == "id" &&
            parent.parentOfType<PsiAnnotation>()?.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION) == true
    }
}
