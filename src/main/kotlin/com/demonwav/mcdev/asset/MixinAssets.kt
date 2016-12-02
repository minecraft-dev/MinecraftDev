/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.asset

object MixinAssets {
    @JvmField val SHADOW = Assets.loadIcon("/assets/icons/mixin/shadow.png")
    @JvmField val SHADOW_DARK = Assets.loadIcon("/assets/icons/mixin/shadow_dark.png")

    @JvmField val MIXIN_CLASS_ICON = Assets.loadIcon("/assets/icons/mixin/mixin_class_gutter.png")
    @JvmField val MIXIN_CLASS_ICON_DARK = Assets.loadIcon("/assets/icons/mixin/mixin_class_gutter_dark.png")
}
