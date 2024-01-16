/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.at.psi.mixins.impl

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory
import com.demonwav.mcdev.platform.mcp.at.psi.getClassFromString
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtArgumentMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class AtArgumentImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtArgumentMixin {

    override val argumentClass
        get() = getClassFromString(argumentText, project)

    override val argumentText: String
        get() = classValue?.text ?: primitive!!.text

    override fun setArgument(argument: String) {
        replace(AtElementFactory.createArgument(project, argument))
    }
}
