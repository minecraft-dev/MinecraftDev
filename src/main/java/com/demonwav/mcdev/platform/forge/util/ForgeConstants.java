package com.demonwav.mcdev.platform.forge.util;

import org.jetbrains.annotations.NotNull;

public final class ForgeConstants {

    @NotNull public static final String SIDE_ONLY_ANNOTATION = "net.minecraftforge.fml.relauncher.SideOnly";
    @NotNull public static final String SIDE_ANNOTATION = "net.minecraftforge.fml.relauncher.Side";
    @NotNull public static final String SIDED_PROXY_ANNOTATION = "net.minecraftforge.fml.common.SidedProxy";
    @NotNull public static final String MOD_ANNOTATION = "net.minecraftforge.fml.common.Mod";

    private ForgeConstants() {
    }
}
