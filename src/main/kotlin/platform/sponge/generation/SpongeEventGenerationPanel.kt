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

package com.demonwav.mcdev.platform.sponge.generation

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.psi.PsiClass
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

class SpongeEventGenerationPanel(chosenClass: PsiClass) : EventGenerationPanel(chosenClass) {

    private val graph = PropertyGraph("SpongeEventGenerationPanel graph")

    private val ignoreCanceledProperty = graph.property(true)
    private val eventOrderProperty = graph.property("DEFAULT")

    override val panel: JPanel by lazy {
        panel {
            row {
                checkBox(MCDevBundle("generate.event_listener.ignore_if_canceled"))
                    .bindSelected(ignoreCanceledProperty)
            }

            row(MCDevBundle("generate.event_listener.event_order")) {
                comboBox(listOf("PRE", "AFTER_PRE", "FIRST", "EARLY", "DEFAULT", "LATE", "LAST", "BEFORE_POST", "POST"))
                    .bindItem(eventOrderProperty)
            }
        }
    }

    override fun gatherData(): GenerationData {
        return SpongeGenerationData(ignoreCanceledProperty.get(), eventOrderProperty.get())
    }
}
