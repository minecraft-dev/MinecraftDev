/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.lang.psi.mixins

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.translations.lang.LangFile
import com.demonwav.mcdev.translations.lang.LangFileType
import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory

abstract class LangEntryImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), LangEntryMixin {
    override val key: String
        get() = node.findChildByType(LangTypes.KEY)?.text ?: ""

    override val value: String
        get() = node.findChildByType(LangTypes.VALUE)?.text ?: ""

    override fun getNameIdentifier() = node.findChildByType(LangTypes.KEY)?.psi

    override fun getName() = key

    override fun setName(name: String): PsiElement {
        val keyElement = node.findChildByType(LangTypes.KEY)
        val tmpFile = PsiFileFactory.getInstance(project).createFileFromText("name", LangFileType, "name=") as LangFile
        val renamed = tmpFile.firstChild as LangEntry
        val newKey = renamed.node.findChildByType(LangTypes.KEY)
        if (newKey != null) {
            if (keyElement != null) {
                this.node.replaceChild(keyElement, newKey)
            } else {
                this.node.addChild(newKey, node.findChildByType(LangTypes.EQUALS))
            }
        } else if (keyElement != null) {
            this.node.removeChild(keyElement)
        }
        return this
    }

    override fun getPresentation() = object : ItemPresentation {
        override fun getPresentableText() = key

        override fun getLocationString() = containingFile.name

        override fun getIcon(unused: Boolean) = PlatformAssets.MINECRAFT_ICON
    }

    override fun toString() = "LangEntry($key=$value)"
}
