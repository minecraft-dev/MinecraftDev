/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
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
        if (getByte() != null) {
            return NbtTypeId.BYTE
        }
        if (getShort() != null) {
            return NbtTypeId.SHORT
        }
        if (getInt() != null) {
            return NbtTypeId.INT
        }
        if (getLong() != null) {
            return NbtTypeId.LONG
        }
        if (getFloat() != null) {
            return NbtTypeId.FLOAT
        }
        if (getDouble() != null) {
            return NbtTypeId.DOUBLE
        }
        if (getByteArray() != null) {
            return NbtTypeId.BYTE_ARRAY
        }
        if (getIntArray() != null) {
            return NbtTypeId.INT_ARRAY
        }
        if (getList() != null) {
            return NbtTypeId.LIST
        }
        if (getCompound() != null) {
            return NbtTypeId.COMPOUND
        }
        if (getString() != null) {
            return NbtTypeId.STRING
        }
        return NbtTypeId.END // Shouldn't actually ever happen
    }
    override fun getTag(): NbttElement? {
        return when (getType()) {
            NbtTypeId.END -> null
            NbtTypeId.BYTE -> getByte()
            NbtTypeId.SHORT -> getShort()
            NbtTypeId.INT -> getInt()
            NbtTypeId.LONG -> getLong()
            NbtTypeId.FLOAT -> getFloat()
            NbtTypeId.DOUBLE -> getDouble()
            NbtTypeId.BYTE_ARRAY -> getByteArray()
            NbtTypeId.STRING -> getString()
            NbtTypeId.LIST -> getList()
            NbtTypeId.COMPOUND -> getCompound()
            NbtTypeId.INT_ARRAY -> getIntArray()
        }
    }
}
