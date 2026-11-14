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

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.util.ThreeState

interface MissingLVTChecker {
    companion object {
        val EP_NAME = ExtensionPointName.create<MissingLVTChecker>("com.demonwav.minecraft-dev.missingLVTChecker")
    }

    /**
     * Returns [ThreeState.YES] if the given class definitely has no local variable table at runtime,
     * [ThreeState.NO] if it definitely does, or [ThreeState.UNSURE] if this cannot be determined.
     *
     * Implementations should return [ThreeState.NO] if they do not handle the given class name. [ThreeState.UNSURE] is
     * for if they are designed to handle the given class name, but were unable to determine whether it's missing an LVT.
     */
    fun hasMissingLVT(context: PsiElement, className: String): ThreeState
}
