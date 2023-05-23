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

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import javax.swing.Icon

object MixinModuleType : AbstractModuleType<MixinModule>("org.spongepowered", "mixin") {

    const val ID = "MIXIN_MODULE_TYPE"

    override val platformType = PlatformType.MIXIN
    override val icon: Icon? = null
    override val id = ID
    override val ignoredAnnotations = emptyList<String>()
    override val listenerAnnotations = emptyList<String>()
    override val hasIcon = false

    override fun generateModule(facet: MinecraftFacet) = MixinModule(facet)
}
