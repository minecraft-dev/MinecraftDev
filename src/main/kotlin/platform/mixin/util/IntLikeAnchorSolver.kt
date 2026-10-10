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

package com.demonwav.mcdev.platform.mixin.util

import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import java.util.EnumSet

/**
 * Represents a constraint solver for the anchor type of a signature containing free int-like types.
 */
class IntLikeAnchorSolver {
    private val candidates = EnumSet.allOf(IntType::class.java)
    private var hasIntPreference = false
    private var hasLeafPreference = false

    /**
     * Adds the given constraint to the solver and returns whether a solution is still possible.
     */
    fun constrain(desiredType: PsiType, isAnchor: Boolean, isHard: Boolean): Boolean {
        val type = IntType.of(desiredType) ?: return false
        when {
            isHard && (isAnchor || type == IntType.INT) -> candidates.retainAll(EnumSet.of(type))
            type == IntType.INT -> hasIntPreference = true
            isAnchor -> candidates.retainAll(EnumSet.of(type))
            else -> {
                candidates.retainAll(EnumSet.of(type, IntType.INT))
                hasLeafPreference = true
            }
        }
        return candidates.isNotEmpty()
    }

    /**
     * Returns either a chosen anchor type or `null` if any anchor type will suffice.
     *
     * **Precondition:** A solution is possible.
     */
    fun solve(): PsiType? {
        require(candidates.isNotEmpty())

        candidates.singleOrNull()?.let { return it.type }

        return when {
            // int preference is the strongest, since choosing a leaf type instead would require a @Coerce *and* the
            // use of an undesired leaf type
            hasIntPreference -> {
                // Cannot have 2 leaves without int
                check(IntType.INT in candidates)
                PsiTypes.intType()
            }
            // Leaf preference is the next strongest, since choosing int instead would require a @Coerce
            // NB leaf preferences can only come from non-anchor positions
            hasLeafPreference -> {
                // Cannot have 2 leaves and a leaf preference
                candidates.single { it != IntType.INT }.type
            }
            else -> {
                // We have multiple options and no preference at all
                check(candidates.size == IntType.entries.size)
                null
            }
        }
    }
}

private enum class IntType(val type: PsiType) {
    INT(PsiTypes.intType()),
    BYTE(PsiTypes.byteType()),
    SHORT(PsiTypes.shortType()),
    CHAR(PsiTypes.charType()),
    BOOLEAN(PsiTypes.booleanType());

    companion object {
        private val lookup = entries.associateBy { it.type }

        fun of(type: PsiType) = lookup[type]
    }
}
