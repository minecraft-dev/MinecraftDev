/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "I18nFoldingSettings", storages = arrayOf(Storage("minecraft_dev.xml")))
class I18nFoldingSettings : PersistentStateComponent<I18nFoldingSettings.State> {

    data class State(
        var isShouldFoldTranslations: Boolean = true
    )

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }

    // State mappings
    var isShouldFoldTranslations: Boolean
        get() = state.isShouldFoldTranslations
        set(isShouldFoldTranslations) {
            state.isShouldFoldTranslations = isShouldFoldTranslations
        }

    companion object {
        val instance: I18nFoldingSettings
            get() = ServiceManager.getService(I18nFoldingSettings::class.java)
    }
}
