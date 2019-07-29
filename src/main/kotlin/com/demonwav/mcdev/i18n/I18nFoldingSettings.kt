/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "I18nFoldingSettings", storages = [(Storage("minecraft_dev.xml"))])
class I18nFoldingSettings : PersistentStateComponent<I18nFoldingSettings.State> {

    data class State(
        var shouldFoldTranslations: Boolean = true
    )

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }

    // State mappings
    var shouldFoldTranslations: Boolean
        get() = state.shouldFoldTranslations
        set(value) {
            state.shouldFoldTranslations = value
        }

    companion object {
        val instance: I18nFoldingSettings
            get() = ServiceManager.getService(I18nFoldingSettings::class.java)
    }
}
