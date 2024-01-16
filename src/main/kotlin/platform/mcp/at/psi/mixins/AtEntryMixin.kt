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

package com.demonwav.mcdev.platform.mcp.at.psi.mixins

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtAsterisk
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtClassName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtKeyword
import com.demonwav.mcdev.platform.mcp.at.psi.AtElement

interface AtEntryMixin : AtElement {

    val asterisk: AtAsterisk?
    val className: AtClassName
    val fieldName: AtFieldName?
    val function: AtFunction?
    val keyword: AtKeyword

    fun setEntry(entry: String)
    fun setKeyword(keyword: AtElementFactory.Keyword)
    fun setClassName(className: String)
    fun setFieldName(fieldName: String)
    fun setFunction(function: String)
    fun setAsterisk()

    fun replaceMember(element: AtElement) {
        // One of these must be true
        when {
            fieldName != null -> fieldName!!.replace(element)
            function != null -> function!!.replace(element)
            asterisk != null -> asterisk!!.replace(element)
            else -> addAfter(className, element)
        }
    }
}
