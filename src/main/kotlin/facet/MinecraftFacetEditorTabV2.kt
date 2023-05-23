/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.enableIf
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingConstants

class MinecraftFacetEditorTabV2(private val configuration: MinecraftFacetConfiguration) : FacetEditorTab() {

    val propertyGraph = PropertyGraph("Minecraft facet editor tab")

    private val platformStates = mutableListOf<PlatformState>()

    val bukkit = platformState(PlatformType.BUKKIT)
    val spigot = platformState(PlatformType.SPIGOT)
    val paper = platformState(PlatformType.PAPER)
    val sponge = platformState(PlatformType.SPONGE)
    val forge = platformState(PlatformType.FORGE)
    val fabric = platformState(PlatformType.FABRIC)
    val architectury = platformState(PlatformType.ARCHITECTURY)
    val mcp = platformState(PlatformType.MCP)
    val mixin = platformState(PlatformType.MIXIN)
    val velocity = platformState(PlatformType.VELOCITY)
    val bungeecord = platformState(PlatformType.BUNGEECORD)
    val waterfall = platformState(PlatformType.WATERFALL)
    val adventure = platformState(PlatformType.ADVENTURE)

    override fun createComponent(): JComponent = panel {
        indent {
            row {
                label("Platform")
                label("Enabled")
                label("Auto detect")
            }.layout(RowLayout.PARENT_GRID)

            createRow(
                bukkit, "Bukkit", PlatformAssets.BUKKIT_ICON_2X,
                { afterChange { _ -> unique(this, spigot.enabled, paper.enabled) } },
                { afterChange { _ -> all(this, spigot.auto, paper.auto)(bukkit, spigot, paper) } },
            )

            createRow(
                spigot, "Spigot", PlatformAssets.SPIGOT_ICON_2X,
                { afterChange { _ -> unique(this, bukkit.enabled, paper.enabled) } },
                { afterChange { _ -> all(this, bukkit.auto, paper.auto)(bukkit, spigot, paper) } },
            )

            createRow(
                paper, "Paper", PlatformAssets.PAPER_ICON_2X,
                { afterChange { _ -> unique(this, bukkit.enabled, spigot.enabled) } },
                { afterChange { _ -> all(this, bukkit.auto, spigot.auto)(bukkit, spigot, paper) } },
            )

            val isDarkMode = UIUtil.isUnderDarcula()
            val spongeIcon = if (isDarkMode) PlatformAssets.SPONGE_ICON_2X_DARK else PlatformAssets.SPONGE_ICON_2X
            createRow(sponge, "Sponge", spongeIcon)

            createRow(
                forge, "Forge", PlatformAssets.FORGE_ICON_2X,
                {
                    afterChange { _ ->
                        also(this, mcp.enabled)
                        unique(this, architectury.enabled)
                    }
                },
                {
                    afterChange { _ ->
                        all(this, fabric.auto, architectury.auto)(forge, fabric, architectury)
                    }
                },
            )

            createRow(
                fabric, "Fabric", PlatformAssets.FABRIC_ICON_2X,
                {
                    afterChange { _ ->
                        also(this, mixin.enabled, mcp.enabled)
                        unique(this, architectury.enabled)
                    }
                },
                {
                    afterChange { _ ->
                        all(this, forge.auto, architectury.auto)(fabric, forge, architectury)
                    }
                },
            )

            createRow(
                architectury, "Architectury", PlatformAssets.ARCHITECTURY_ICON_2X,
                { afterChange { _ -> unique(this, fabric.enabled, forge.enabled) } },
                {
                    afterChange { _ ->
                        all(this, forge.auto, fabric.auto)(architectury, forge, fabric)
                    }
                },
            )

            val mcpIcon = if (isDarkMode) PlatformAssets.MCP_ICON_2X_DARK else PlatformAssets.MCP_ICON_2X
            createRow(mcp, "MCP", mcpIcon)

            val mixinIcon = if (isDarkMode) PlatformAssets.MIXIN_ICON_2X_DARK else PlatformAssets.MIXIN_ICON_2X
            createRow(
                mixin, "Mixin", mixinIcon,
                { afterChange { _ -> also(this, mcp.enabled) } }
            )

            createRow(velocity, "Velocity", PlatformAssets.VELOCITY_ICON_2X)

            createRow(
                bungeecord, "BungeeCord", PlatformAssets.BUNGEECORD_ICON_2X,
                { afterChange { _ -> unique(this, waterfall.enabled) } }
            )

            createRow(
                waterfall, "Waterfall", PlatformAssets.WATERFALL_ICON_2X,
                { afterChange { _ -> unique(this, bungeecord.enabled) } }
            )

            createRow(adventure, "Adventure", PlatformAssets.ADVENTURE_ICON_2X)
        }
    }

    override fun getDisplayName() = "Minecraft Module Settings"

    override fun isModified(): Boolean {
        var modified = false

        runOnAll { enabled, auto, platformType, userTypes, _ ->
            modified += auto.get() == platformType in userTypes
            modified += !auto.get() && enabled.get() != userTypes[platformType]
        }

        return modified
    }

    override fun reset() {
        runOnAll { enabled, auto, platformType, userTypes, autoTypes ->
            auto.set(platformType !in userTypes)
            enabled.set(userTypes[platformType] ?: (platformType in autoTypes))
        }
    }

    override fun apply() {
        configuration.state.userChosenTypes.clear()
        runOnAll { enabled, auto, platformType, userTypes, _ ->
            if (!auto.get()) {
                userTypes[platformType] = enabled.get()
            }
        }
    }

    private inline fun runOnAll(
        run: (
            GraphProperty<Boolean>,
            GraphProperty<Boolean>,
            PlatformType,
            MutableMap<PlatformType, Boolean>,
            Set<PlatformType>
        ) -> Unit,
    ) {
        val state = configuration.state
        for (platformState in platformStates) {
            run(
                platformState.enabled,
                platformState.auto,
                platformState.platform,
                state.userChosenTypes,
                state.autoDetectTypes,
            )
        }
    }

    private fun unique(vararg checkBoxes: GraphProperty<Boolean>) {
        if (checkBoxes.size <= 1) {
            return
        }

        if (checkBoxes[0].get()) {
            for (i in 1 until checkBoxes.size) {
                checkBoxes[i].set(false)
            }
        }
    }

    private fun also(vararg checkBoxes: GraphProperty<Boolean>) {
        if (checkBoxes.size <= 1) {
            return
        }

        if (checkBoxes[0].get()) {
            for (i in 1 until checkBoxes.size) {
                checkBoxes[i].set(true)
            }
        }
    }

    private fun all(vararg checkBoxes: GraphProperty<Boolean>): Invoker {
        if (checkBoxes.size <= 1) {
            return Invoker()
        }

        for (i in 1 until checkBoxes.size) {
            if (checkBoxes[i].get() != checkBoxes[0].get()) {
                checkBoxes[i].set(checkBoxes[0].get())
            }
        }

        return object : Invoker() {
            override fun invoke(vararg platformStates: PlatformState) {
                for (platformState in platformStates) {
                    checkAuto(platformState.auto, platformState.enabled, platformState.platform)
                }
            }
        }
    }

    private fun checkAuto(auto: GraphProperty<Boolean>, enabled: GraphProperty<Boolean>, type: PlatformType) {
        if (auto.get()) {
            enabled.set(type in configuration.state.autoDetectTypes)
        }
    }

    private operator fun Boolean.plus(n: Boolean) = this || n

    // This is here so we can use vararg. Can't use parameter modifiers in function type definitions for some reason
    open class Invoker {
        open operator fun invoke(vararg platformStates: PlatformState) {}
    }

    data class PlatformState(
        val index: Int,
        val platform: PlatformType,
        val enabled: GraphProperty<Boolean>,
        val auto: GraphProperty<Boolean>,
    )

    private fun platformState(platform: PlatformType): PlatformState {
        val enabledProperty = propertyGraph.property(
            configuration.state.userChosenTypes[platform] ?: configuration.state.autoDetectTypes.contains(platform)
        )
        val autoProperty = propertyGraph.property(!configuration.state.autoDetectTypes.contains(platform))

        val platformState = PlatformState(platformStates.size, platform, enabledProperty, autoProperty)
        platformStates.add(platformState)

        return platformState
    }

    private fun Panel.createRow(
        platform: PlatformState,
        label: String,
        icon: Icon,
        enabledCheckbox: GraphProperty<Boolean>.() -> Unit = {},
        autoCheckbox: GraphProperty<Boolean>.() -> Unit = {},
    ): Row = row(JLabel(label, icon, SwingConstants.LEADING)) {
        platform.enabled.apply(enabledCheckbox)
        platform.auto.afterChange { _ -> checkAuto(platform.auto, platform.enabled, platform.platform) }
        platform.auto.apply(autoCheckbox)
        checkBox("").bindSelected(platform.enabled).enableIf(platform.auto.not())
        checkBox("").bindSelected(platform.auto)
    }.layout(RowLayout.PARENT_GRID)
}
