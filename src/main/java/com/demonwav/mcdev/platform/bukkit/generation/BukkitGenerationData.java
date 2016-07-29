package com.demonwav.mcdev.platform.bukkit.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;

import org.jetbrains.annotations.NotNull;

public class BukkitGenerationData implements GenerationData {
    private final boolean ignoreCanceled;
    private final String eventPriority;

    private BukkitGenerationData(boolean ignoredCanceled, @NotNull String eventPriority) {
        this.ignoreCanceled = ignoredCanceled;
        this.eventPriority = eventPriority;
    }

    public boolean isIgnoreCanceled() {
        return ignoreCanceled;
    }

    public String getEventPriority() {
        return eventPriority;
    }

    public static BukkitGenerationDataBuilder builder() {
        return new BukkitGenerationDataBuilder();
    }

    static class BukkitGenerationDataBuilder {
        private boolean ignoreCanceled;
        private String eventPriority;

        public BukkitGenerationDataBuilder ignoreCanceled(boolean ignoreCanceled) {
            this.ignoreCanceled = ignoreCanceled;
            return this;
        }

        public BukkitGenerationDataBuilder eventPriority(@NotNull String eventPriority) {
            this.eventPriority = eventPriority;
            return this;
        }

        public BukkitGenerationData build() {
            return new BukkitGenerationData(ignoreCanceled, eventPriority);
        }
    }
}
