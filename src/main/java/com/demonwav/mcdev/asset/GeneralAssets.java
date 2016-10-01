package com.demonwav.mcdev.asset;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

@SuppressWarnings("unused")
public final class GeneralAssets extends Assets {

    @NotNull public static final Icon LISTENER = loadIcon("/assets/icons/general/EventListener_dark.png");
    @NotNull public static final Icon PLUGIN = loadIcon("/assets/icons/general/plugin.png");

    private GeneralAssets() {}
}
