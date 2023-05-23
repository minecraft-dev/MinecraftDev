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

package com.demonwav.mcdev.platform.mixin.inspection.implements

import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationAttributeInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.IMPLEMENTS
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue

class DuplicateInterfacePrefixInspection : MixinAnnotationAttributeInspection(IMPLEMENTS, null) {

    override fun getStaticDescription() = "Reports duplicate @Interface prefixes in an @Implements annotation."

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder,
    ) {
        val interfaces = value.findAnnotations().ifEmpty { return }

        val prefixes = ArrayList<String>()
        for (iface in interfaces) {
            val prefixValue = iface.findDeclaredAttributeValue("prefix") ?: continue
            val prefix = prefixValue.constantStringValue ?: continue
            if (prefix in prefixes) {
                holder.registerProblem(prefixValue, "Duplicate prefix '$prefix'")
            } else {
                prefixes.add(prefix)
            }
        }
    }
}
