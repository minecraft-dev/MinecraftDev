/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
