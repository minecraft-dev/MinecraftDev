/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;
import org.jetbrains.annotations.NotNull;

public class CanaryGenerationData implements GenerationData {

    private final boolean ignoreCanceled;
    private final String priority;

    public CanaryGenerationData(boolean ignoredCanceled, @NotNull String priority) {
        this.ignoreCanceled = ignoredCanceled;
        this.priority = priority;
    }

    public boolean isIgnoreCanceled() {
        return ignoreCanceled;
    }

    public String getPriority() {
        return priority;
    }

}
