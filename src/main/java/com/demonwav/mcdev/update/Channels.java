package com.demonwav.mcdev.update;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum Channels {
    SCOTLIN("Kotlin And Scala Support", "https://plugins.jetbrains.com/plugins/kotlin%20and%20scala%20support/8327", 1),
    MIXINS("Mixins", "https://plugins.jetbrains.com/plugins/mixins/8327", 2);

    private final String title;
    private final String url;
    private final int index;

    Channels(final String title, final String url, final int index) {
        this.title = title;
        this.url = url;
        this.index = index;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public int getIndex() {
        return index;
    }

    @Nullable
    public static Channels getChannel(int index) {
        for (Channels channels : values()) {
            if (channels.getIndex() == index) {
                return channels;
            }
        }

        return null;
    }

    public static List<Channels> orderedList() {
        return ImmutableList.of(SCOTLIN, MIXINS);
    }

    public boolean hasChannel() {
        return UpdateSettings.getInstance().getPluginHosts().contains(url);
    }
}
