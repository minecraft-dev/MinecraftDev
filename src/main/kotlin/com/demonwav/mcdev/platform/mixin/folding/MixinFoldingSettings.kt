/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.folding

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "MixinFoldingSettings", storages = [Storage("minecraft_dev.xml")])
class MixinFoldingSettings : PersistentStateComponent<MixinFoldingSettings.State> {

    data class State(
        var foldTargetDescriptors: Boolean = true,
        var foldObjectCasts: Boolean = false
    )

    private var state = State()

    override fun getState(): State = this.state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        val instance: MixinFoldingSettings
            get() = ServiceManager.getService(MixinFoldingSettings::class.java)
    }
}
