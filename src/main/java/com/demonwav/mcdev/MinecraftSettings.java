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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.editor.markup.EffectType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@State(
        name = "MinecraftSettings",
        storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/minecraft_dev.xml")
)
public class MinecraftSettings implements PersistentStateComponent<MinecraftSettingsState> {

    @NotNull
    private MinecraftSettingsState state = new MinecraftSettingsState();

    @NotNull
    public static MinecraftSettings getInstance() {
        return ServiceManager.getService(MinecraftSettings.class);
    }

    @NotNull
    @Override
    public MinecraftSettingsState getState() {
        return state;
    }

    @Override
    public void loadState(MinecraftSettingsState state) {
        this.state = state;
    }

    // State mappings
    public boolean isShowProjectPlatformIcons() {
        return state.isShowProjectPlatformIcons();
    }

    public void setShowProjectPlatformIcons(boolean showProjectPlatformIcons) {
        state.setShowProjectPlatformIcons(showProjectPlatformIcons);
    }

    public boolean isShowEventListenerGutterIcons() {
        return state.isShowEventListenerGutterIcons();
    }

    public void setShowEventListenerGutterIcons(boolean showEventListenerGutterIcons) {
        state.setShowEventListenerGutterIcons(showEventListenerGutterIcons);
    }

    public boolean isShowChatColorGutterIcons() {
        return state.isShowChatColorGutterIcons();
    }

    public void setShowChatColorGutterIcons(boolean showChatColorGutterIcons) {
        state.setShowChatColorGutterIcons(showChatColorGutterIcons);
    }

    public boolean isShowChatColorUnderlines() {
        return state.isShowChatColorUnderlines();
    }

    public void setShowChatColorUnderlines(boolean showChatColorUnderlines) {
        state.setShowChatColorUnderlines(showChatColorUnderlines);
    }

    @NotNull
    public UnderlineType getUnderlineType() {
        return state.getUnderlineType();
    }

    public void setUnderlineType(@NotNull UnderlineType underlineType) {
        state.setUnderlineType(underlineType);
    }

    public int getUnderlineTypeIndex() {
        final UnderlineType type = getUnderlineType();
        for (int i = 0; i < UnderlineType.values().length; i++) {
            if (type == UnderlineType.values()[i]) {
                return i;
            }
        }
        return 0;
    }

    public boolean isEnableSideOnlyChecks() {
        return state.isEnableSideOnlyChecks();
    }

    public void setEnableSideOnlyChecks(boolean enableSideOnlyChecks) {
        state.setEnableSideOnlyChecks(enableSideOnlyChecks);
    }

    public enum UnderlineType {

        NORMAL("Normal", EffectType.LINE_UNDERSCORE),
        BOLD("Bold", EffectType.BOLD_LINE_UNDERSCORE),
        DOTTED("Dotted", EffectType.BOLD_DOTTED_LINE),
        BOXED("Boxed", EffectType.BOXED),
        ROUNDED_BOXED("Rounded Boxed", EffectType.ROUNDED_BOX),
        WAVED("Waved", EffectType.WAVE_UNDERSCORE);

        private final String regular;
        private final EffectType effectType;

        UnderlineType(@NotNull final String regular, @NotNull final EffectType effectType) {
            this.regular = regular;
            this.effectType = effectType;
        }

        @NotNull
        @Override
        @Contract(pure = true)
        public String toString() {
            return regular;
        }

        @NotNull
        public EffectType getEffectType() {
            return effectType;
        }
    }
}
