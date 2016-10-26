/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev;

import org.jetbrains.annotations.NotNull;

public class MinecraftSettingsState {

    private boolean showProjectPlatformIcons = true;
    private boolean showEventListenerGutterIcons = true;
    private boolean showChatColorGutterIcons = true;
    private boolean showChatColorUnderlines = false;

    private boolean enableSideOnlyChecks = true;

    @NotNull
    private MinecraftSettings.UnderlineType underlineType = MinecraftSettings.UnderlineType.DOTTED;

    public boolean isShowProjectPlatformIcons() {
        return showProjectPlatformIcons;
    }

    public void setShowProjectPlatformIcons(boolean showProjectPlatformIcons) {
        this.showProjectPlatformIcons = showProjectPlatformIcons;
    }

    public boolean isShowEventListenerGutterIcons() {
        return showEventListenerGutterIcons;
    }

    public void setShowEventListenerGutterIcons(boolean showEventListenerGutterIcons) {
        this.showEventListenerGutterIcons = showEventListenerGutterIcons;
    }

    public boolean isShowChatColorGutterIcons() {
        return showChatColorGutterIcons;
    }

    public void setShowChatColorGutterIcons(boolean showChatColorGutterIcons) {
        this.showChatColorGutterIcons = showChatColorGutterIcons;
    }

    public boolean isShowChatColorUnderlines() {
        return showChatColorUnderlines;
    }

    public void setShowChatColorUnderlines(boolean showChatColorUnderlines) {
        this.showChatColorUnderlines = showChatColorUnderlines;
    }

    @NotNull
    public MinecraftSettings.UnderlineType getUnderlineType() {
        return underlineType;
    }

    public void setUnderlineType(@NotNull MinecraftSettings.UnderlineType underlineType) {
        this.underlineType = underlineType;
    }

    public boolean isEnableSideOnlyChecks() {
        return enableSideOnlyChecks;
    }

    public void setEnableSideOnlyChecks(boolean enableSideOnlyChecks) {
        this.enableSideOnlyChecks = enableSideOnlyChecks;
    }
}
