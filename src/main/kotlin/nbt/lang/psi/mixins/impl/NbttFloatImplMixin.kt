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

package com.demonwav.mcdev.nbt.lang.psi.mixins.impl

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttFloatMixin
import com.demonwav.mcdev.nbt.tags.TagFloat
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttFloatImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttFloatMixin {

    override fun getFloatTag(): TagFloat {
        // Can't just regex out the f, since "Infinity" contains an f
        if (text.contains("Infinity")) {
            return TagFloat(text.trim().let { it.substring(0, it.length - 1) }.toFloat())
        }
        return TagFloat(text.trim().replace(fRegex, "").toFloat())
    }

    companion object {
        private val fRegex = Regex("[fF]")
    }
}
