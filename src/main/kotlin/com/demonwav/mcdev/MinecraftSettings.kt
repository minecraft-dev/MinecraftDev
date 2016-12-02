/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.editor.markup.EffectType
import org.jetbrains.annotations.Contract

@State(name = "MinecraftSettings", storages = arrayOf(Storage(file = StoragePathMacros.APP_CONFIG + "/minecraft_dev.xml")))
class MinecraftSettings : PersistentStateComponent<MinecraftSettingsState> {

    private var state = MinecraftSettingsState()

    override fun getState(): MinecraftSettingsState {
        return state
    }

    override fun loadState(state: MinecraftSettingsState) {
        this.state = state
    }

    // State mappings
    var isShowProjectPlatformIcons: Boolean
        get() = state.isShowProjectPlatformIcons
        set(showProjectPlatformIcons) {
            state.isShowProjectPlatformIcons = showProjectPlatformIcons
        }

    var isShowEventListenerGutterIcons: Boolean
        get() = state.isShowEventListenerGutterIcons
        set(showEventListenerGutterIcons) {
            state.isShowEventListenerGutterIcons = showEventListenerGutterIcons
        }

    var isShowChatColorGutterIcons: Boolean
        get() = state.isShowChatColorGutterIcons
        set(showChatColorGutterIcons) {
            state.isShowChatColorGutterIcons = showChatColorGutterIcons
        }

    var isShowChatColorUnderlines: Boolean
        get() = state.isShowChatColorUnderlines
        set(showChatColorUnderlines) {
            state.isShowChatColorUnderlines = showChatColorUnderlines
        }

    var underlineType: UnderlineType
        get() = state.underlineType
        set(underlineType) {
            state.underlineType = underlineType
        }

    val underlineTypeIndex: Int
        get() {
            val type = underlineType
            for (i in 0..UnderlineType.values().size - 1) {
                if (type == UnderlineType.values()[i]) {
                    return i
                }
            }
            return 0
        }

    var isEnableSideOnlyChecks: Boolean
        get() = state.isEnableSideOnlyChecks
        set(enableSideOnlyChecks) {
            state.isEnableSideOnlyChecks = enableSideOnlyChecks
        }

    enum class UnderlineType(private val regular: String, val effectType: EffectType) {

        NORMAL("Normal", EffectType.LINE_UNDERSCORE),
        BOLD("Bold", EffectType.BOLD_LINE_UNDERSCORE),
        DOTTED("Dotted", EffectType.BOLD_DOTTED_LINE),
        BOXED("Boxed", EffectType.BOXED),
        ROUNDED_BOXED("Rounded Boxed", EffectType.ROUNDED_BOX),
        WAVED("Waved", EffectType.WAVE_UNDERSCORE);

        @Contract(pure = true)
        override fun toString(): String {
            return regular
        }
    }

    companion object {
        val instance: MinecraftSettings
            get() = ServiceManager.getService(MinecraftSettings::class.java)
    }
}
