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

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected

class UseMixinsStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    val useMixinsProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.useMixins")
    var useMixins by useMixinsProperty

    override fun setupUI(builder: Panel) {
        with(builder) {
            row(MCDevBundle("creator.ui.use_mixins.label")) {
                checkBox("")
                    .bindSelected(useMixinsProperty)
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, useMixins)
    }

    companion object {
        val KEY = Key.create<Boolean>("${UseMixinsStep::class.java.name}.useMixins")
    }
}
