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

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

abstract class ConfigPropertyInspection(private vararg val names: String) : MixinConfigInspection() {

    protected abstract fun visitValue(literal: JsonStringLiteral, holder: ProblemsHolder)

    protected open fun findProperty(literal: PsiElement) =
        (literal.parent as? JsonProperty)?.takeIf { it.value === literal }

    final override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private inner class Visitor(private val holder: ProblemsHolder) : JsonElementVisitor() {

        override fun visitStringLiteral(literal: JsonStringLiteral) {
            val property = findProperty(literal) ?: return
            if (property.name !in names) {
                return
            }

            visitValue(literal, holder)
        }
    }
}
