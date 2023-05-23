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
