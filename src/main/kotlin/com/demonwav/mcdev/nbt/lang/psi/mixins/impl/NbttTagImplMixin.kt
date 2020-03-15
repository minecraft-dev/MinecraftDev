/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
