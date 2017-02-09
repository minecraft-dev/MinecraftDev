/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.util;

import org.jetbrains.annotations.NotNull;

public final class ForgeConstants {

    @NotNull public static final String SIDE_ONLY_ANNOTATION = "net.minecraftforge.fml.relauncher.SideOnly";
    @NotNull public static final String SIDE_ANNOTATION = "net.minecraftforge.fml.relauncher.Side";
    @NotNull public static final String SIDED_PROXY_ANNOTATION = "net.minecraftforge.fml.common.SidedProxy";
    @NotNull public static final String MOD_ANNOTATION = "net.minecraftforge.fml.common.Mod";
    @NotNull public static final String EVENT_HANDLER_ANNOTATION = "net.minecraftforge.fml.common.Mod.EventHandler";
    @NotNull public static final String SUBSCRIBE_EVENT_ANNOTATION = "net.minecraftforge.fml.common.eventhandler.SubscribeEvent";
    @NotNull public static final String FML_EVENT = "net.minecraftforge.fml.common.event.FMLEvent";
    @NotNull public static final String EVENT = "net.minecraftforge.fml.common.eventhandler.Event";
    @NotNull public static final String MCMOD_INFO = "mcmod.info";

    private ForgeConstants() {
    }
}
