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

package com.demonwav.mcdev.platform.mixin.expression.psi

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.mixin.expression.MEExpressionFileType
import com.demonwav.mcdev.platform.mixin.expression.MEExpressionLanguage
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEDeclarationItem
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEItem
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatementItem
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class MEExpressionFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MEExpressionLanguage) {
    override fun getFileType() = MEExpressionFileType
    override fun toString() = "MixinExtras Expression File"
    override fun getIcon(flags: Int) = PlatformAssets.MIXIN_ICON

    val items: Array<MEItem> get() = findChildrenByClass(MEItem::class.java)
    val declarations: List<MEDeclarationItem> get() = items.filterIsInstance<MEDeclarationItem>()
    val statements: List<MEStatement> get() = items.mapNotNull { (it as? MEStatementItem)?.statement }
}
