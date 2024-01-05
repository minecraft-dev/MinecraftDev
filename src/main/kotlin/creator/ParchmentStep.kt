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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.step.AbstractMcVersionChainStep
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.and
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.util.application
import javax.swing.DefaultComboBoxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class ParchmentStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

    val useParchmentProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.useParchment")
    var useParchment by useParchmentProperty

    val parchmentVersionProperty = propertyGraph.property<String?>(null)
    var parchmentVersion by parchmentVersionProperty

    val hasParchmentVersionProperty = propertyGraph.property(false)
    var hasParchmentVersion by hasParchmentVersionProperty

    private val parchmentVersionsModel = DefaultComboBoxModel<String>(emptyArray())

    init {
        storeToData()
        val mcVersionProperty = findStep<AbstractMcVersionChainStep>().getVersionProperty(0)
        mcVersionProperty.afterChange { mcVersion ->
            updateVersions(mcVersion.toString())
        }

        updateVersions(mcVersionProperty.get().toString())
    }

    private fun updateVersions(mcVersion: String) {
        parchmentVersionsModel.removeAllElements()
        application.executeOnPooledThread {
            runBlocking {
                val versions = ParchmentVersion.downloadData(mcVersion)
                hasParchmentVersion = versions != null && versions.versions.isNotEmpty()
                if (versions != null) {
                    withContext(Dispatchers.Swing) {
                        parchmentVersionsModel.removeAllElements()
                        parchmentVersionsModel.addAll(versions.versions)
                        parchmentVersionsModel.selectedItem = versions.versions.firstOrNull { !it.contains('-') }
                            ?: versions.versions.firstOrNull()
                    }
                }
            }
        }
    }

    override fun setupUI(builder: Panel) {
        with(builder) {
            row(MCDevBundle("creator.ui.parchment.label")) {
                checkBox("")
                    .bindSelected(useParchmentProperty)

                comboBox(parchmentVersionsModel)
                    .enabledIf(useParchmentProperty and hasParchmentVersionProperty)
                    .bindItem(parchmentVersionProperty)
            }
        }

        super.setupUI(builder)
    }

    override fun setupProject(project: Project) {
        data.putUserData(USE_KEY, useParchment)
        data.putUserData(VERSION_KEY, parchmentVersion)
    }

    companion object {
        val USE_KEY = Key.create<Boolean>("${ParchmentStep::class.java.name}.useParchment")
        val VERSION_KEY = Key.create<String>("${ParchmentStep::class.java}.parchmentVersion")
    }
}
