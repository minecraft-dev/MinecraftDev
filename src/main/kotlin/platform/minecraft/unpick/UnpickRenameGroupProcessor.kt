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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor

/**
 * A group may be defined in several places, renames all of them if one of them is renamed.
 */
class UnpickRenameGroupProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement) = element is UnpickGroupDefinition && element.name != null

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        val definition = element as? UnpickGroupDefinition ?: return
        for (other in UnpickGroups.findDefinitions(definition, definition.name ?: return)) {
            if (other != definition) {
                allRenames[other] = newName
            }
        }
    }
}
