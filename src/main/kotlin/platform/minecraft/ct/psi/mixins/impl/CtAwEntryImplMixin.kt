/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.ct.psi.mixins.impl

import com.demonwav.mcdev.platform.minecraft.ct.gen.psi.CtTypes
import com.demonwav.mcdev.platform.minecraft.ct.psi.mixins.CtAwEntryMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

abstract class CtAwEntryImplMixin(node: ASTNode) : CtEntryImplMixin(node), CtAwEntryMixin {

    override val accessKind: String?
        get() = findChildByType<PsiElement>(CtTypes.ACCESS)?.text

    override val targetClassName: String?
        get() = findChildByType<PsiElement>(CtTypes.CLASS_NAME)?.text
}
