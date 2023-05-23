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

package com.demonwav.mcdev.platform.mcp.at.psi.mixins.impl

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtEntryMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class AtEntryImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtEntryMixin {

    override fun setEntry(entry: String) {
        replace(AtElementFactory.createEntry(project, entry))
    }

    override fun setKeyword(keyword: AtElementFactory.Keyword) {
        this.keyword.replace(AtElementFactory.createKeyword(project, keyword))
    }

    override fun setClassName(className: String) {
        this.className.replace(AtElementFactory.createClassName(project, className))
    }

    override fun setFieldName(fieldName: String) {
        val newField = AtElementFactory.createFieldName(project, fieldName)
        replaceMember(newField)
    }

    override fun setFunction(function: String) {
        val atFunction = AtElementFactory.createFunction(project, function)
        replaceMember(atFunction)
    }

    override fun setAsterisk() {
        val asterisk = AtElementFactory.createAsterisk(project)
        replaceMember(asterisk)
    }
}
