/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;

import org.jetbrains.annotations.NotNull;

public class SpongeGenerationData implements GenerationData {

    private final boolean ignoreCanceled;
    private final String eventOrder;

    public SpongeGenerationData(boolean ignoredCanceled, @NotNull String eventOrder) {
        this.ignoreCanceled = ignoredCanceled;
        this.eventOrder = eventOrder;
    }

    public boolean isIgnoreCanceled() {
        return ignoreCanceled;
    }

    public String getEventOrder() {
        return eventOrder;
    }
}
