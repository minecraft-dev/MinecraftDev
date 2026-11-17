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

package com.demonwav.mcdev.platform.minecraft.unpick.eval

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.Nls

/**
 * A problem found while evaluating a group constant. Each kind of problem is reported by a separate inspection.
 */
sealed class UnpickProblem(val element: PsiElement, @Nls val message: String) {
    /** A problem with the field referenced by a field expression. */
    class Field(element: PsiElement, @Nls message: String) : UnpickProblem(element, message)

    /** An operation applied to incompatible types, or a constant incompatible with its group. */
    class Type(element: PsiElement, @Nls message: String) : UnpickProblem(element, message)

    /** Integral division or remainder by zero. */
    class DivisionByZero(element: PsiElement, @Nls message: String) : UnpickProblem(element, message)

    override fun equals(other: Any?): Boolean {
        return other is UnpickProblem && other.javaClass == javaClass && other.element == element &&
            other.message == message
    }

    override fun hashCode() = 31 * element.hashCode() + message.hashCode()
}
