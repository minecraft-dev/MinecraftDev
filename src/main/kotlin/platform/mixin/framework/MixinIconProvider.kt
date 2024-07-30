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
import com.intellij.ide.IconLayerProvider
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiClass
import javax.swing.Icon

class MixinIconProvider : IconLayerProvider {
    override fun getLayerIcon(element: Iconable, isLocked: Boolean): Icon? =
        MixinAssets.MIXIN_MARK.takeIf {
            MinecraftSettings.instance.mixinClassIcon && element is PsiClass && element.isMixin
        }

    override fun getLayerDescription(): String =
        "Mixin class"
}
