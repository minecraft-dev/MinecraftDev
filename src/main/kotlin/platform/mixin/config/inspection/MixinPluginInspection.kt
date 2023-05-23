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

package com.demonwav.mcdev.platform.mixin.config.inspection

import com.demonwav.mcdev.platform.mixin.config.reference.MixinPlugin
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.MIXIN_PLUGIN
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.util.PsiUtil

class MixinPluginInspection : ConfigPropertyInspection("plugin") {

    override fun getStaticDescription() = "Reports invalid Mixin plugins in Mixin configuration files."

    override fun visitValue(literal: JsonStringLiteral, holder: ProblemsHolder) {
        val pluginClass = MixinPlugin.resolve(literal) ?: return
        if (pluginClass !is PsiClass) {
            holder.registerProblem(literal, "Mixin plugin class expected")
            return
        }

        val pluginInterface = MixinPlugin.findInterface(literal) ?: return
        if (!pluginClass.isInheritor(pluginInterface, true)) {
            holder.registerProblem(literal, "Class does not extend ${PsiNameHelper.getShortClassName(MIXIN_PLUGIN)}")
            return
        }

        if (!PsiUtil.isInstantiatable(pluginClass)) {
            holder.registerProblem(literal, "Plugin class is not instantiable")
        }
    }
}
