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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttStringMixin
import com.demonwav.mcdev.nbt.tags.TagString
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.apache.commons.lang3.StringUtils

abstract class NbttStringImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttStringMixin {

    override fun getStringTag(): TagString {
        return TagString(getStringValue())
    }

    override fun getStringValue() = getNbtStringValue(text)
}

fun getNbtStringValue(text: String): String {
    val noQuotes = if (text.startsWith('"')) {
        text.let { it.substring(1, it.length - 1) }
    } else {
        text
    }

    return StringUtils.replaceEach(noQuotes, arrayOf("\\\\", "\\n", "\\\"", "\\t"), arrayOf("\\", "\n", "\"", "\t"))
}
