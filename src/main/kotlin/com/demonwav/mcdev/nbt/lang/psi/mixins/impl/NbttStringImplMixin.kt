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
