/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.toml

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlValue

abstract class TomlElementVisitor : PsiElementVisitor() {

    override fun visitElement(element: PsiElement) = when (element) {
        is TomlKeyValue -> visitKeyValue(element)
        is TomlKeySegment -> visitKeySegment(element)
        is TomlValue -> visitValue(element)
        else -> super.visitElement(element)
    }

    open fun visitKeyValue(keyValue: TomlKeyValue) = Unit

    open fun visitKeySegment(keySegment: TomlKeySegment) = Unit

    open fun visitValue(value: TomlValue) = Unit
}
