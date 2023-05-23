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

package com.demonwav.mcdev.creator.step

import com.demonwav.mcdev.util.License
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem

class LicenseStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    val licenseProperty = propertyGraph.property(License.ALL_RIGHTS_RESERVED.id)
        .bindStorage("${javaClass.name}.license")
    var license by licenseProperty

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("License:") {
                comboBox(License.values().toList())
                    .bindItem(licenseProperty.transform({ License.byId(it) ?: License.ALL_RIGHTS_RESERVED }) { it.id })
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, License.byId(license))
    }

    companion object {
        val KEY = Key.create<License>("${LicenseStep::class.java.name}.license")
    }
}
