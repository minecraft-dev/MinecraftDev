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

package com.demonwav.mcdev.platform.fabric.annotater

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.nio.file.Path
import kotlin.io.path.Path

@State(name = "MinecraftAnnotaterSettings", storages = [Storage("minecraft_dev.xml")])
class AnnotaterSettings : PersistentStateComponent<AnnotaterSettings.State> {

    data class State(
        var enabled: Boolean = false,
        var showAnnotaterJavadocs: Boolean = true,
        var shortenJavadocClassNames: Boolean = true,
        var checkoutMode: CheckoutMode = CheckoutMode.MANAGED,
        var managedRoot: String = "",
        var customDirectory: String = "",
    )

    enum class CheckoutMode {
        /**
         * The plugin clones annotater itself and keeps one git worktree per Minecraft version.
         */
        MANAGED,

        /**
         * The user points the plugin at an existing annotater checkout, which the plugin never runs git in.
         */
        CUSTOM,
    }

    private var state = State()

    override fun getState() = state

    override fun loadState(state: State) {
        this.state = state
    }

    var enabled: Boolean
        get() = state.enabled
        set(enabled) {
            state.enabled = enabled
        }

    var showAnnotaterJavadocs: Boolean
        get() = state.showAnnotaterJavadocs
        set(showAnnotaterJavadocs) {
            state.showAnnotaterJavadocs = showAnnotaterJavadocs
        }

    var shortenJavadocClassNames: Boolean
        get() = state.shortenJavadocClassNames
        set(shortenJavadocClassNames) {
            state.shortenJavadocClassNames = shortenJavadocClassNames
        }

    var checkoutMode: CheckoutMode
        get() = state.checkoutMode
        set(checkoutMode) {
            state.checkoutMode = checkoutMode
        }

    var managedRoot: String
        get() = state.managedRoot
        set(managedRoot) {
            state.managedRoot = managedRoot
        }

    var customDirectory: String
        get() = state.customDirectory
        set(customDirectory) {
            state.customDirectory = customDirectory
        }

    val effectiveManagedRoot: Path
        get() = state.managedRoot.takeIf { it.isNotBlank() }?.let(::Path) ?: defaultManagedRoot

    companion object {
        val defaultManagedRoot: Path
            get() = PathManager.getSystemDir().resolve("minecraft-dev").resolve("annotater")

        val instance: AnnotaterSettings
            get() = ApplicationManager.getApplication().getService(AnnotaterSettings::class.java)
    }
}
