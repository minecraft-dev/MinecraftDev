/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.asset;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

@SuppressWarnings("unused")
public final class MixinAssets extends Assets {

    @NotNull public static final Icon SHADOW = loadIcon("/assets/icons/mixin/shadow.png");
    @NotNull public static final Icon SHADOW_DARK = loadIcon("/assets/icons/mixin/shadow_dark.png");

    @NotNull public static final Icon MIXIN_CLASS_ICON = loadIcon("/assets/icons/mixin/mixin_class_gutter.png");
    @NotNull public static final Icon MIXIN_CLASS_ICON_DARK = loadIcon("/assets/icons/mixin/mixin_class_gutter_dark.png");

    private MixinAssets() {}
}
