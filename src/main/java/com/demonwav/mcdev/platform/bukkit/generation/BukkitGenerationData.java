/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.bukkit.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;

import org.jetbrains.annotations.NotNull;

public class BukkitGenerationData implements GenerationData {
    private final boolean ignoreCanceled;
    private final String eventPriority;

    public BukkitGenerationData(boolean ignoredCanceled, @NotNull String eventPriority) {
        this.ignoreCanceled = ignoredCanceled;
        this.eventPriority = eventPriority;
    }

    public boolean isIgnoreCanceled() {
        return ignoreCanceled;
    }

    public String getEventPriority() {
        return eventPriority;
    }
}
