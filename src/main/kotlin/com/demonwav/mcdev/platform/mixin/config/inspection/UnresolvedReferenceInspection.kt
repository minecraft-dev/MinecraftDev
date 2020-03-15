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

import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElementVisitor

class UnresolvedReferenceInspection : MixinConfigInspection() {

    override fun getStaticDescription() = "Reports unresolved references in Mixin configuration files."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JsonElementVisitor() {

        override fun visitStringLiteral(literal: JsonStringLiteral) {
            for (reference in literal.references) {
                if (reference !is InspectionReference) {
                    continue
                }

                if (reference.unresolved) {
                    holder.registerProblem(
                        literal, "Cannot resolve ${reference.description}".format(reference.canonicalText),
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, reference.rangeInElement
                    )
                }
            }
        }
    }
}
