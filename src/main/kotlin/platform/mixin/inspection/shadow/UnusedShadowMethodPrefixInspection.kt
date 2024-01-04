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

package com.demonwav.mcdev.platform.mixin.inspection.shadow

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.quickfix.RemoveAnnotationAttributeQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod

class UnusedShadowMethodPrefixInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports unused prefixes of @Shadow methods"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitMethod(method: PsiMethod) {
            val shadow = method.findAnnotation(SHADOW) ?: return
            val prefixValue = shadow.findDeclaredAttributeValue("prefix") ?: return
            val prefix = prefixValue.constantStringValue ?: return

            if (!method.name.startsWith(prefix)) {
                holder.registerProblem(
                    prefixValue,
                    "Unused @Shadow prefix",
                    RemoveAnnotationAttributeQuickFix("@Shadow", "prefix"),
                )
            }
        }
    }
}
