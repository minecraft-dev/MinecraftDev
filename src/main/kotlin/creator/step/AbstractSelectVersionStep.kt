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

package com.demonwav.mcdev.creator.step

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem

abstract class AbstractSelectVersionStep<T : Comparable<T>>(
    parent: NewProjectWizardStep,
    val versions: List<T>,
) : AbstractNewProjectWizardStep(parent) {
    protected abstract val label: String

    val versionProperty = propertyGraph.property("")
        .bindStorage("${javaClass.name}.selectedVersion")
    var version by versionProperty

    protected lateinit var versionBox: ComboBox<String>

    override fun setupUI(builder: Panel) {
        with(builder) {
            row(label) {
                setupRow(this)
            }
        }
    }

    open fun setupRow(builder: Row) {
        with(builder) {
            val box = comboBox(versions.sortedDescending().map(Any::toString)).bindItem(versionProperty)
            val selectedItem = box.component.selectedItem
            if (selectedItem is String) {
                version = selectedItem
            }
            versionBox = box.component

            // fix the selection to the latest version if it was previously at the latest version
            val props = PropertiesComponent.getInstance()
            val latestVersionProp = "${javaClass.name}.latestVersion"
            val prevLatestVersion = props.getValue(latestVersionProp)
            val latestVersion = versions.maxOrNull()?.toString()
            if (version == prevLatestVersion) {
                version = latestVersion ?: ""
            }
            props.setValue(latestVersionProp, latestVersion)
        }
    }
}
