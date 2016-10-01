package com.demonwav.mcdev.asset;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

@SuppressWarnings("unused")
public final class MixinAssets extends Assets {

    @NotNull public static final Icon SHADOW = loadIcon("/assets/icons/mixin/shadow.png");
    @NotNull public static final Icon SHADOW_DARK = loadIcon("/assets/icons/mixin/shadow_dark.png");

    private MixinAssets() {}
}
