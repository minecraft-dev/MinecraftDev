/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.sponge.util;

import org.jetbrains.annotations.NotNull;

public final class SpongeConstants {

    @NotNull public static final String PLUGIN_ANNOTATION = "org.spongepowered.api.plugin.Plugin";
    @NotNull public static final String TEXT_COLORS = "org.spongepowered.api.text.format.TextColors";
    @NotNull public static final String TEXT_FORMATTING = "net.minecraft.util.text.TextFormatting";
    @NotNull public static final String LISTENER_ANNOTATION = "org.spongepowered.api.event.Listener";

    private SpongeConstants() {
    }
}
