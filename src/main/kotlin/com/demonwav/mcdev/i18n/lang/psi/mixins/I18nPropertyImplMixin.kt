/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang.psi.mixins

import com.demonwav.mcdev.i18n.lang.I18nElementFactory
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

abstract class I18nPropertyImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), I18nPropertyMixin {
    override fun getKey() = node.findChildByType(I18nTypes.KEY)?.text ?: ""

    override fun getValue() = node.findChildByType(I18nTypes.VALUE)?.text ?: ""

    override fun getNameIdentifier() = node.findChildByType(I18nTypes.KEY)?.psi

    override fun getName() = getKey()

    override fun setName(name: String): PsiElement {
        val keyElement = node.findChildByType(I18nTypes.KEY)
        val renamed = I18nElementFactory.createProperty(project, name)
        val newKey = renamed.node.findChildByType(I18nTypes.KEY)
        if (newKey != null) {
            if (keyElement != null)
                this.node.replaceChild(keyElement, newKey)
            else
                this.node.addChild(newKey, node.findChildByType(I18nTypes.EQUALS))
        } else if (keyElement != null) {
            this.node.removeChild(keyElement)
        }
        return this
    }
}