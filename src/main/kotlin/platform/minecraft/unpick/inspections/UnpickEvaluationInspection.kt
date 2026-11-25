/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick.inspections

import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickExpressionEvaluator
import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickProblem
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupConstant
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

/**
 * Reports one kind of problem found by evaluating group constants.
 */
abstract class UnpickEvaluationInspection(private val problemClass: Class<out UnpickProblem>) : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : UnpickVisitor() {
            override fun visitGroupConstant(element: UnpickGroupConstant) {
                for (problem in UnpickExpressionEvaluator.evaluate(element).problems) {
                    if (problemClass.isInstance(problem)) {
                        holder.registerProblem(problem.element, problem.message)
                    }
                }
            }
        }
}

class InvalidUnpickConstantFieldInspection : UnpickEvaluationInspection(UnpickProblem.Field::class.java)

class UnpickExpressionTypeInspection : UnpickEvaluationInspection(UnpickProblem.Type::class.java)

class UnpickDivisionByZeroInspection : UnpickEvaluationInspection(UnpickProblem.DivisionByZero::class.java)
