/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.util

import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.controlFlow.AnalysisCanceledException
import com.intellij.psi.controlFlow.ControlFlowFactory
import com.intellij.psi.controlFlow.ControlFlowUtil

@Throws(AnalysisCanceledException::class)
fun hasImplicitReturnStatement(body: PsiCodeBlock): Boolean {
    val controlFlow = ControlFlowFactory.getControlFlowNoConstantEvaluate(body)
    return ControlFlowUtil.canCompleteNormally(controlFlow, 0, controlFlow.size)
}
