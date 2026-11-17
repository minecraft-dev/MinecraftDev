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

import com.demonwav.mcdev.platform.minecraft.unpick.GroupFormat
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupScope
import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickElement
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface UnpickGroupDefinitionMixin : UnpickElement, PsiNameIdentifierOwner, NavigatablePsiElement {
    val isDefaultGroup: Boolean
        get() = name == null

    val scopes: List<UnpickGroupScope>

    val isStrict: Boolean

    val isFlags: Boolean

    val format: GroupFormat?

    /**
     * The contents of the `#:` doc comments directly preceding this group definition, or `null` if there are none.
     */
    val docComment: String?
}
