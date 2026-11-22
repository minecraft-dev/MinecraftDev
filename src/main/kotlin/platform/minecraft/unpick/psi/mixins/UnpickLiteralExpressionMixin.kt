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

package com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins

import com.demonwav.mcdev.platform.minecraft.unpick.DataType
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickLiterals
import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickElement

interface UnpickLiteralExpressionMixin : UnpickElement {
    /**
     * Whether this literal is the direct operand of a unary minus.
     */
    val isNegated: Boolean

    val dataType: DataType?

    val parseResult: UnpickLiterals.Result

    /**
     * The value of this literal, or `null` if it is malformed. Like the reference implementation, a negated literal
     * evaluates to its positive value, which may have overflowed.
     */
    val value: Any?
        get() = (parseResult as? UnpickLiterals.Result.Value)?.value
}
