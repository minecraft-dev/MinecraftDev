/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.impl

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.minecraft.unpick.GroupFormat
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFlagsAttribute
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFormatAttribute
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupScope
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickScopeAttribute
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickStrictAttribute
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickElementFactory
import com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.UnpickGroupDefinitionMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import com.intellij.util.IncorrectOperationException

abstract class UnpickGroupDefinitionImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), UnpickGroupDefinitionMixin {
    private val self: UnpickGroupDefinition
        get() = this as UnpickGroupDefinition

    override fun getName(): String? = self.groupName?.text

    override fun setName(name: String): PsiElement {
        val identifier = self.groupName?.firstChild ?: throw IncorrectOperationException("Cannot rename default group")
        identifier.replace(UnpickElementFactory.createIdentifier(project, name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? = self.groupName

    override fun getTextOffset(): Int = self.groupName?.textOffset ?: super.getTextOffset()

    override val scopes: List<UnpickGroupScope>
        get() = self.groupAttributeList.mapNotNull { (it as? UnpickScopeAttribute)?.groupScope }

    override val isStrict: Boolean
        get() = self.groupAttributeList.any { it is UnpickStrictAttribute }

    override val isFlags: Boolean
        get() = self.groupAttributeList.any { it is UnpickFlagsAttribute }

    override val format: GroupFormat?
        get() = self.groupAttributeList.firstNotNullOfOrNull { attribute ->
            (attribute as? UnpickFormatAttribute)?.formatName?.text?.let(GroupFormat::fromKeyword)
        }

    override val docComment: String?
        get() {
            val lines = mutableListOf<String>()
            var sibling = prevSibling
            while (sibling is PsiWhiteSpace || sibling is PsiComment) {
                if (sibling is PsiComment) {
                    if (sibling.elementType != UnpickTypes.DOC_COMMENT) {
                        break
                    }
                    lines += sibling.text.removePrefix("#:").trimStart()
                }
                sibling = sibling.prevSibling
            }
            return if (lines.isEmpty()) null else lines.asReversed().joinToString("\n")
        }

    override fun getPresentation(): ItemPresentation {
        val dataType = self.dataType?.text.orEmpty()
        return PresentationData(
            name?.let { "$dataType $it" } ?: dataType,
            containingFile?.name,
            PlatformAssets.MINECRAFT_ICON,
            null,
        )
    }
}
