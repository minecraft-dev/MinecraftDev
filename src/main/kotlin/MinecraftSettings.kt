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
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "MinecraftSettings", storages = [Storage("minecraft_dev.xml")])
class MinecraftSettings : PersistentStateComponent<MinecraftSettings> {
    override fun getState() = this

    override fun loadState(state: MinecraftSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    var isShowProjectPlatformIcons = true
    var isShowEventListenerGutterIcons = true
    var isShowChatColorGutterIcons = true
    var isShowChatColorUnderlines = false

    var underlineType = UnderlineType.DOTTED

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
