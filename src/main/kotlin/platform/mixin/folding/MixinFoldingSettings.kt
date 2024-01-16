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

package com.demonwav.mcdev.platform.mixin.folding

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "MixinFoldingSettings", storages = [Storage("minecraft_dev.xml")])
class MixinFoldingSettings : PersistentStateComponent<MixinFoldingSettings.State> {

    data class State(
        var foldTargetDescriptors: Boolean = true,
        var foldObjectCasts: Boolean = false,
        var foldInvokerCasts: Boolean = true,
        var foldInvokerMethodCalls: Boolean = true,
        var foldAccessorCasts: Boolean = true,
        var foldAccessorMethodCalls: Boolean = false,
    )

    private var state = State()

    override fun getState(): State = this.state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        val instance: MixinFoldingSettings
            get() = ApplicationManager.getApplication().getService(MixinFoldingSettings::class.java)
    }
}
