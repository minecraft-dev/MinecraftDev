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

package com.demonwav.mcdev.platform.bungeecord.generation

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.psi.PsiClass
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

class BungeeCordEventGenerationPanel(chosenClass: PsiClass) : EventGenerationPanel(chosenClass) {

    private val graph = PropertyGraph("BungeeCordEventGenerationPanel graph")

    private val eventPriorityProperty = graph.property("NORMAL")

    override val panel: JPanel by lazy {
        panel {
            row(MCDevBundle("generate.event_listener.event_priority")) {
                comboBox(listOf("HIGHEST", "HIGH", "NORMAL", "LOW", "LOWEST"))
                    .bindItem(eventPriorityProperty)
            }
        }
    }

    override fun gatherData() = BungeeCordGenerationData(eventPriorityProperty.get())
}
