/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.markup.EffectType

@State(name = "MinecraftSettings", storages = [Storage("minecraft_dev.xml")])
class MinecraftSettings : PersistentStateComponent<MinecraftSettings.State> {

    data class State(
        var isShowProjectPlatformIcons: Boolean = true,
        var isShowEventListenerGutterIcons: Boolean = true,
        var isShowChatColorGutterIcons: Boolean = true,
        var isShowChatColorUnderlines: Boolean = false,
        var underlineType: UnderlineType = UnderlineType.DOTTED,

        var isShadowAnnotationsSameLine: Boolean = true,
    )

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
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

    var isShadowAnnotationsSameLine: Boolean
        get() = state.isShadowAnnotationsSameLine
        set(shadowAnnotationsSameLine) {
            state.isShadowAnnotationsSameLine = shadowAnnotationsSameLine
        }

    enum class UnderlineType(private val regular: String, val effectType: EffectType) {

        NORMAL("Normal", EffectType.LINE_UNDERSCORE),
        BOLD("Bold", EffectType.BOLD_LINE_UNDERSCORE),
        DOTTED("Dotted", EffectType.BOLD_DOTTED_LINE),
        BOXED("Boxed", EffectType.BOXED),
        ROUNDED_BOXED("Rounded Boxed", EffectType.ROUNDED_BOX),
        WAVED("Waved", EffectType.WAVE_UNDERSCORE),
        ;

        override fun toString(): String {
            return regular
        }
    }

    companion object {
        val instance: MinecraftSettings
            get() = ApplicationManager.getApplication().getService(MinecraftSettings::class.java)
    }
}
