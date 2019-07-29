/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.asset

@Suppress("unused")
object MixinAssets : Assets() {
    val SHADOW = loadIcon("/assets/icons/mixin/shadow.png")
    val SHADOW_DARK = loadIcon("/assets/icons/mixin/shadow_dark.png")

    val MIXIN_CLASS_ICON = loadIcon("/assets/icons/mixin/mixin_class_gutter.png")
    val MIXIN_CLASS_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_class_gutter_dark.png")
}
