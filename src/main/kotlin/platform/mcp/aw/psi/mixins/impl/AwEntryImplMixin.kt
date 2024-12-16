/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.aw.psi.mixins.impl

import com.demonwav.mcdev.platform.mcp.aw.AwElementFactory
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwEntryMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

abstract class AwEntryImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AwEntryMixin {

    override val accessKind: String
        get() = findNotNullChildByType<PsiElement>(AwTypes.ACCESS_ELEMENT).text

    override val targetClassName: String?
        get() = findChildByType<PsiElement>(AwTypes.CLASS_NAME)?.text

    override val comment: PsiComment?
        get() = PsiTreeUtil.skipWhitespacesForward(this) as? PsiComment

    override val commentText: String?
        get() = comment?.text?.substring(1)

    override fun setComment(text: String?) {
        if (text == null) {
            comment?.delete()
            return
        }

        val newComment = AwElementFactory.createComment(project, text)
        val existingComment = comment
        if (existingComment == null) {
            parent.addAfter(newComment, this)
            return
        }

        existingComment.replace(newComment)
    }
}
