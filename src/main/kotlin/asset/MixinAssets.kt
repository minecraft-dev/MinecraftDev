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

package com.demonwav.mcdev.asset

@Suppress("unused")
object MixinAssets : Assets() {
    val MIXIN_ACCESSOR_ICON = loadIcon("/assets/icons/mixin/mixin_accessor.svg")
    val MIXIN_ACCESSOR_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_accessor_dark.svg")

    val MIXIN_ACCESSOR_MIXIN_ICON = loadIcon("/assets/icons/mixin/mixin_accessor_mixin.svg")
    val MIXIN_ACCESSOR_MIXIN_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_accessor_mixin_dark.svg")

    val MIXIN_CLASS_ICON = loadIcon("/assets/icons/mixin/mixin_class_gutter.svg")
    val MIXIN_CLASS_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_class_gutter_dark.svg")

    val MIXIN_ELEMENT_ICON = loadIcon("/assets/icons/mixin/mixin_element.svg")
    val MIXIN_ELEMENT_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_element_dark.svg")

    val MIXIN_INJECTOR_ICON = loadIcon("/assets/icons/mixin/mixin_injector.svg")
    val MIXIN_INJECTOR_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_injector_dark.svg")

    val MIXIN_SHADOW_ICON = loadIcon("/assets/icons/mixin/mixin_shadow.svg")
    val MIXIN_SHADOW_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_shadow_dark_alt.svg")

    val MIXIN_ENUM_CONSTANT_ICON = loadIcon("/assets/icons/mixin/mixin_enum_constant.svg")
    val MIXIN_ENUM_CONSTANT_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_enum_constant_dark.svg")

    val MIXIN_TARGET_ACCESSOR_MIXIN_ICON = loadIcon("/assets/icons/mixin/mixin_target_accessor_mixin.svg")
    val MIXIN_TARGET_ACCESSOR_MIXIN_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_target_accessor_mixin_dark.svg")

    val MIXIN_TARGET_CLASS_ICON = loadIcon("/assets/icons/mixin/mixin_target_class_gutter.svg")
    val MIXIN_TARGET_CLASS_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_target_class_gutter_dark.svg")

    val MIXIN_MARK = loadIcon("/assets/icons/mixin/mixin_mark.svg")
    val ACCESSOR_MARK = loadIcon("/assets/icons/mixin/accessor_mark.svg")
}
