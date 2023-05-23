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

import com.demonwav.mcdev.nbt.lang.psi.NbttElement
import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttTagMixin
import com.demonwav.mcdev.nbt.tags.NbtTypeId
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttTagImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttTagMixin {

    override fun getType(): NbtTypeId {
        return when {
            getByte() != null -> NbtTypeId.BYTE
            getShort() != null -> NbtTypeId.SHORT
            getInt() != null -> NbtTypeId.INT
            getLong() != null -> NbtTypeId.LONG
            getFloat() != null -> NbtTypeId.FLOAT
            getDouble() != null -> NbtTypeId.DOUBLE
            getByteArray() != null -> NbtTypeId.BYTE_ARRAY
            getIntArray() != null -> NbtTypeId.INT_ARRAY
            getLongArray() != null -> NbtTypeId.LONG_ARRAY
            getList() != null -> NbtTypeId.LIST
            getCompound() != null -> NbtTypeId.COMPOUND
            getString() != null -> NbtTypeId.STRING
            else -> NbtTypeId.END // Shouldn't actually ever happen
        }
    }

    override fun getTag(): NbttElement? {
        return getByte()
            ?: getShort()
            ?: getInt()
            ?: getLong()
            ?: getFloat()
            ?: getDouble()
            ?: getByteArray()
            ?: getString()
            ?: getList()
            ?: getCompound()
            ?: getIntArray()
            ?: getLongArray()
    }
}
