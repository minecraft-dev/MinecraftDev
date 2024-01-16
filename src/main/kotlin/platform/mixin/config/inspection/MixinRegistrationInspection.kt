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

package com.demonwav.mcdev.platform.mixin.config.inspection

import com.demonwav.mcdev.platform.mixin.config.reference.MixinClass
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

class MixinRegistrationInspection : ConfigPropertyInspection("mixins", "server", "client") {

    override fun getStaticDescription() = "Reports invalid Mixin classes in Mixin configuration files."

    // Literal -> Array -> Property
    override fun findProperty(literal: PsiElement) = literal.parent?.let { super.findProperty(it) }

    override fun visitValue(literal: JsonStringLiteral, holder: ProblemsHolder) {
        val mixinClass = MixinClass.resolve(literal) ?: return
        if (mixinClass !is PsiClass) {
            holder.registerProblem(literal, "Mixin class expected")
            return
        }

        if (!mixinClass.isMixin) {
            holder.registerProblem(literal, "Specified class is not a @Mixin class")
        }
    }
}
