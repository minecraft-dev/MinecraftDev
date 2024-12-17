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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtClassName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.demonwav.mcdev.platform.mcp.at.psi.AtElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

abstract class AtElementManipulator<E : AtElement>(val factory: (Project, String) -> E) :
    AbstractElementManipulator<E>() {

    override fun handleContentChange(element: E, range: TextRange, newContent: String): E? {
        val text = element.text
        val newText = text.substring(0, range.startOffset) + newContent + text.substring(range.endOffset)
        @Suppress("UNCHECKED_CAST")
        return element.replace(factory(element.project, newText)) as E
    }
}

class AtClassNameElementManipulator : AtElementManipulator<AtClassName>(AtElementFactory::createClassName)

class AtFieldNameElementManipulator : AtElementManipulator<AtFieldName>(AtElementFactory::createFieldName)

class AtFuncNameElementManipulator : AtElementManipulator<AtFunction>(AtElementFactory::createFunction)
