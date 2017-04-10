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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttTagNameMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttTagNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttTagNameMixin {

    override fun getTagName(): String {
        val noQuotes = if (text.startsWith("\"")) {
            text.let { it.substring(1, it.length - 1) }
        } else {
            text
        }

        return noQuotes
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\t", "\t")
    }
}
