/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang.psi.mixins

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.i18n.I18nElementFactory
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement

abstract class I18nEntryImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), I18nEntryMixin {
    override val key: String
        get() = node.findChildByType(I18nTypes.KEY)?.text ?: ""

    override val value: String
        get() = node.findChildByType(I18nTypes.VALUE)?.text ?: ""

    override fun getNameIdentifier() = node.findChildByType(I18nTypes.KEY)?.psi

    override fun getName() = key

    override fun setName(name: String): PsiElement {
        val keyElement = node.findChildByType(I18nTypes.KEY)
        val renamed = I18nElementFactory.createEntry(project, name)
        val newKey = renamed.node.findChildByType(I18nTypes.KEY)
        if (newKey != null) {
            if (keyElement != null) {
                this.node.replaceChild(keyElement, newKey)
            } else {
                this.node.addChild(newKey, node.findChildByType(I18nTypes.EQUALS))
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

    override fun toString() = "I18nEntry($key=$value)"
}
