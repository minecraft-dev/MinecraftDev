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

package com.demonwav.mcdev.platform.mixin.framework

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinAnnotation
import com.intellij.ide.IconProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.ui.LayeredIcon
import javax.swing.Icon

class MixinIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int) : Icon? =
        if (MinecraftSettings.instance.mixinClassIcon && element is PsiClass && element.isMixin)
            LayeredIcon.create(element.withoutMixin().getIcon(flags), MixinAssets.MIXIN_MARK)
        else null

    private fun PsiClass.withoutMixin(): PsiClass =
       (copy() as PsiClass).apply { mixinAnnotation?.delete() }
}
