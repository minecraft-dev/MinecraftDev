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
    @NotNull public static final String LISTENER_ANNOTATION = "org.spongepowered.api.event.Listener";
    @NotNull public static final String IS_CANCELLED_ANNOTATION = "org.spongepowered.api.event.filter.IsCancelled";
    @NotNull public static final String CANCELLABLE = "org.spongepowered.api.event.Cancellable";
    @NotNull public static final String EVENT_ISCANCELLED_METHOD_NAME = "isCancelled";

    private SpongeConstants() {
    }
}
