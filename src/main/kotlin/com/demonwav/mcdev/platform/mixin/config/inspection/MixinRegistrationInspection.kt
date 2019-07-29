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
