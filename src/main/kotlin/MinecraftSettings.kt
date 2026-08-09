/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Text
import com.intellij.util.xmlb.annotations.Transient

@State(name = "MinecraftSettings", storages = [Storage("minecraft_dev.xml")])
class MinecraftSettings : PersistentStateComponent<MinecraftSettings.State> {

    data class State(
        var isShowProjectPlatformIcons: Boolean = true,
        var isShowEventListenerGutterIcons: Boolean = true,
        var isShowChatColorGutterIcons: Boolean = true,
        var isShowChatColorUnderlines: Boolean = false,
        var underlineType: UnderlineType = UnderlineType.DOTTED,
        var forceExternalAnnotations: Boolean = true,

        var mixinClassIcon: Boolean = true,

        var creatorTemplateRepos: List<TemplateRepo> = listOf(TemplateRepo.makeBuiltinRepo()),
        var creatorMavenRepos: List<MavenRepo> = listOf(),
    )

    @Tag("repo")
    data class TemplateRepo(
        @get:Attribute("name")
        var name: String,
        @get:Attribute("provider")
        var provider: String,
        @get:Text
        var data: String
    ) {
        constructor() : this("", "", "")

        companion object {

            fun makeBuiltinRepo(): TemplateRepo {
                return TemplateRepo(MCDevBundle("minecraft.settings.creator.repo.builtin_name"), "builtin", "true")
            }
        }
    }

    @Tag("maven")
    data class MavenRepo(
        @get:Attribute("id")
        var id: String,
        @get:Attribute("url")
        var url: String,
        @get:Attribute("username")
        var username: String,
        @get:Transient // Saved in PasswordSafe
        var password: String?
    ) {
        constructor() : this("", "", "", "")
    }

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
        if (state.creatorTemplateRepos.isEmpty()) {
            state.creatorTemplateRepos = listOf(TemplateRepo.makeBuiltinRepo())
        }
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

    var forceExternalAnnotations: Boolean
        get() = state.forceExternalAnnotations
        set(forceExternalAnnotations) {
            state.forceExternalAnnotations = forceExternalAnnotations
        }

    var mixinClassIcon: Boolean
        get() = state.mixinClassIcon
        set(mixinClassIcon) {
            state.mixinClassIcon = mixinClassIcon
        }

    var creatorTemplateRepos: List<TemplateRepo>
        get() = state.creatorTemplateRepos.map { it.copy() }
        set(creatorTemplateRepos) {
            state.creatorTemplateRepos = creatorTemplateRepos.ifEmpty { listOf(TemplateRepo.makeBuiltinRepo()) }
                .map { it.copy() }
        }

    var creatorMavenRepos: List<MavenRepo>
        get() = state.creatorMavenRepos.map { it.copy() }
        set(creatorMavenRepos) {
            state.creatorMavenRepos = creatorMavenRepos.map { it.copy() }
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
