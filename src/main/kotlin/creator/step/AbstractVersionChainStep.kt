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

import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStepBase
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem

private class VersionProperties(
    val step: AbstractVersionChainStep,
    val versionProperties: Array<ObservableMutableProperty<Comparable<*>>>,
    val preferredVersions: Array<MutableMap<List<Comparable<*>>, Comparable<*>>>,
) {
    init {
        loadPreferredVersions()

        var propertyChangeCount = 0L

        for ((i, prop) in versionProperties.withIndex()) {
            prop.afterChange { value ->
                val prevPropertyChangeCount = ++propertyChangeCount

                val versionsAbove = versionProperties.take(i).map(ObservableMutableProperty<Comparable<*>>::get)
                val newestVersion = step.getAvailableVersions(versionsAbove).sortedDescending().first()
                if (value == newestVersion) {
                    preferredVersions[i].remove(versionsAbove)
                } else {
                    preferredVersions[i][versionsAbove] = value
                }

                for (j in (i + 1) until versionProperties.size) {
                    val versionsAboveChild =
                        versionProperties.take(j).map(ObservableMutableProperty<Comparable<*>>::get)
                    val preferredVersion = preferredVersions[j][versionsAboveChild]
                    step.comboBoxes?.let { comboBoxes ->
                        step.setSelectableItems(j, step.getAvailableVersions(versionsAboveChild).sortedDescending())
                        if (preferredVersion != null) {
                            comboBoxes[j].selectedItem = preferredVersion
                        } else {
                            comboBoxes[j].selectedIndex = 0
                        }
                    } ?: run {
                        versionProperties[j].set(
                            preferredVersion ?: step.getAvailableVersions(versionsAboveChild).first(),
                        )
                    }

                    // the above code could have triggered a recursive property change which would have dealt with the
                    // rest of what we're going to do here
                    if (propertyChangeCount != prevPropertyChangeCount) {
                        return@afterChange
                    }
                }

                savePreferredVersions()
            }
        }
    }

    private fun savePreferredVersions() {
        val stateComponent = PreferredVersionStateComponent.getInstance()
        val preferredVersions = this.preferredVersions.map { m ->
            m.map { (key, value) -> key.map(Comparable<*>::toString) to value.toString() }.toMap()
        }
        stateComponent.set("${step.javaClass.name}.preferredVersions", preferredVersions)
    }

    private fun loadPreferredVersions() {
        val stateComponent = PreferredVersionStateComponent.getInstance()
        val preferredVersions = stateComponent.get("${step.javaClass.name}.preferredVersions") ?: return
        for ((i, preferences) in preferredVersions.withIndex()) {
            if (i >= this.preferredVersions.size) {
                break
            }

            preferenceEntryLoop@
            for ((versionsAbove, version) in preferences) {
                if (versionsAbove.size != i) {
                    continue@preferenceEntryLoop
                }

                val parsedVersionsAbove = mutableListOf<Comparable<*>>()
                for (versionAbove in versionsAbove) {
                    parsedVersionsAbove += step.getAvailableVersions(parsedVersionsAbove)
                        .firstOrNull { it.toString() == versionAbove }
                        ?: continue@preferenceEntryLoop
                }
                val parsedVersion = step.getAvailableVersions(parsedVersionsAbove)
                    .firstOrNull { it.toString() == version }
                    ?: continue@preferenceEntryLoop

                this.preferredVersions[i][parsedVersionsAbove] = parsedVersion
            }

            val preferredVersion =
                this.preferredVersions[i][versionProperties.take(i).map(ObservableMutableProperty<Comparable<*>>::get)]
            if (preferredVersion != null) {
                versionProperties[i].set(preferredVersion)
            }
        }
    }
}

/**
 * This class replaces chains of [AbstractNewProjectWizardMultiStepBase]s. The problem with the latter approach is that
 * widgets become improperly aligned.
 */
abstract class AbstractVersionChainStep(
    parent: NewProjectWizardStep,
    private vararg val labels: String,
) : AbstractNewProjectWizardStep(parent) {
    private val versionProperties by lazy {
        val versionProperties = mutableListOf<ObservableMutableProperty<Comparable<*>>>()
        for (i in labels.indices) {
            versionProperties += propertyGraph.property(
                getAvailableVersions(versionProperties.map(ObservableMutableProperty<Comparable<*>>::get)).first(),
            )
        }
        val preferredVersions = labels.indices.map { mutableMapOf<List<Comparable<*>>, Comparable<*>>() }
        VersionProperties(this, versionProperties.toTypedArray(), preferredVersions.toTypedArray())
    }

    internal var comboBoxes: Array<VersionChainComboBox>? = null

    abstract fun getAvailableVersions(versionsAbove: List<Comparable<*>>): List<Comparable<*>>

    fun getVersionProperty(index: Int) = versionProperties.versionProperties[index]

    fun getVersion(index: Int) = versionProperties.versionProperties[index].get()

    fun getVersionBox(index: Int) = comboBoxes?.let { it[index] }

    open fun setSelectableItems(index: Int, items: List<Comparable<*>>) {
        getVersionBox(index)!!.setSelectableItems(items)
    }

    open fun createComboBox(row: Row, index: Int, items: List<Comparable<*>>): Cell<VersionChainComboBox> {
        return row.cell(VersionChainComboBox(items))
    }

    override fun setupUI(builder: Panel) {
        val comboBoxes = mutableListOf<VersionChainComboBox>()
        with(builder) {
            for ((i, label) in labels.withIndex()) {
                row(label) {
                    val comboBox = createComboBox(
                        this,
                        i,
                        getAvailableVersions(
                            versionProperties.versionProperties
                                .take(i).map(ObservableMutableProperty<Comparable<*>>::get),
                        ).sortedDescending(),
                    ).bindItem(versionProperties.versionProperties[i])
                    comboBoxes += comboBox.component
                }
            }
        }
        this.comboBoxes = comboBoxes.toTypedArray()
    }
}

class VersionChainComboBox(items: List<Comparable<*>>) : ComboBox<Comparable<*>>() {
    init {
        setSelectableItems(items)
    }

    fun setSelectableItems(items: List<Comparable<*>>) {
        val currentItem = selectedItem
        model = CollectionComboBoxModel(items)
        if (selectedItem != currentItem) {
            // changing the model doesn't fire item change, which we want to receive
            selectedItemChanged()
        }
    }
}

private typealias PreferredVersionStateValue = List<Map<List<String>, String>>

@Service
@State(
    name = "PreferredVersions",
    storages = [Storage("mcdev.CreatorPreferredVersions.xml", roamingType = RoamingType.DISABLED)],
)
class PreferredVersionStateComponent : PersistentStateComponent<MutableMap<String, PreferredVersionStateValue>> {
    private var state = mutableMapOf<String, PreferredVersionStateValue>()

    fun get(key: String) = state[key]

    fun set(key: String, value: PreferredVersionStateValue) {
        state[key] = value
    }

    override fun getState() = state
    override fun loadState(state: MutableMap<String, PreferredVersionStateValue>) {
        this.state = state
    }

    companion object {
        fun getInstance() = service<PreferredVersionStateComponent>()
    }
}

private fun List<Comparable<*>>.sortedDescending(): List<Comparable<*>> {
    fun <T : Comparable<T>> sortImpl(list: List<Comparable<*>>): List<Comparable<*>> {
        @Suppress("UNCHECKED_CAST")
        return (list as List<T>).sortedByDescending { it }
    }
    // pretend we're strings to make the compiler happy
    return sortImpl<String>(this)
}
