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
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.icons.AllIcons
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.util.and
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.content.AlertIcon
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.util.application
import com.intellij.util.ui.AsyncProcessIcon
import javax.swing.DefaultComboBoxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class ParchmentStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

    val useParchmentProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.useParchment")
    var useParchment by useParchmentProperty

    val parchmentVersionProperty = propertyGraph.property<ParchmentVersion?>(null)
    var parchmentVersion by parchmentVersionProperty

    val parchmentVersionsProperty = propertyGraph.property<List<ParchmentVersion>>(emptyList())
    var parchmentVersions by parchmentVersionsProperty

    val loadingVersionsProperty = propertyGraph.property(false)
    var loadingVersions by loadingVersionsProperty

    val hasParchmentVersionProperty = propertyGraph.property(false)
    var hasParchmentVersion by hasParchmentVersionProperty

    val includeOlderMcVersionsProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.includeOlderMcVersions")
    var includeOlderMcVersions by includeOlderMcVersionsProperty

    val includeSnapshotsVersionsProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.includeSnapshotsVersions")
    var includeSnapshotsVersions by includeSnapshotsVersionsProperty

    private val parchmentVersionsModel = DefaultComboBoxModel<ParchmentVersion>(emptyArray())
    private val mcVersionProperty: ObservableMutableProperty<SemanticVersion>

    init {
        storeToData()
        @Suppress("UNCHECKED_CAST")
        mcVersionProperty = findStep<AbstractMcVersionChainStep>().getVersionProperty(0)
            as ObservableMutableProperty<SemanticVersion>
        mcVersionProperty.afterChange { updateVersionsModel() }

        includeOlderMcVersionsProperty.afterChange { updateVersionsModel() }
        includeSnapshotsVersionsProperty.afterChange { updateVersionsModel() }

        downloadVersions()
    }

    private fun updateVersionsModel() {
        val mcVersion = mcVersionProperty.get()
        val versions = parchmentVersions.filter { version ->
            if (!includeOlderMcVersions && version.mcVersion < mcVersion) {
                return@filter false
            }

            if (!includeSnapshotsVersions && version.parchmentVersion.contains("-SNAPSHOT")) {
                return@filter false
            }

            return@filter true
        }

        hasParchmentVersion = versions.isNotEmpty()

        parchmentVersionsModel.removeAllElements()
        parchmentVersionsModel.addAll(versions)

        parchmentVersionsModel.selectedItem = versions.firstOrNull { !it.parchmentVersion.contains('-') }
            ?: versions.firstOrNull()

        loadingVersions = false
    }

    private fun downloadVersions() {
        loadingVersions = true
        parchmentVersionsModel.removeAllElements()
        application.executeOnPooledThread {
            runBlocking {
                val versions = ParchmentVersion.downloadData()
                withContext(Dispatchers.Swing) {
                    parchmentVersions = versions
                    updateVersionsModel()
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

                cell(AsyncProcessIcon("$javaClass.parchmentVersions"))
                    .visibleIf(loadingVersionsProperty)

                label(MCDevBundle("creator.ui.parchment.no_version.message"))
                    .visibleIf(hasParchmentVersionProperty.not() and loadingVersionsProperty.not())
                    .applyToComponent { icon = AlertIcon(AllIcons.General.Warning) }
            }

            row(MCDevBundle("creator.ui.parchment.include.label")) {
                checkBox(MCDevBundle("creator.ui.parchment.include.old_mc.label"))
                    .enabledIf(useParchmentProperty)
                    .bindSelected(includeOlderMcVersionsProperty)
                checkBox(MCDevBundle("creator.ui.parchment.include.snapshots.label"))
                    .enabledIf(useParchmentProperty)
                    .bindSelected(includeSnapshotsVersionsProperty)
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
        val VERSION_KEY = Key.create<ParchmentVersion>("${ParchmentStep::class.java}.parchmentVersion")
    }
}
