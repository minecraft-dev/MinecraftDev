/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem

abstract class AbstractSelectVersionStep(parent: NewProjectWizardStep, val versions: List<SemanticVersion>) : AbstractNewProjectWizardStep(parent) {
    protected abstract val label: String

    val versionProperty = propertyGraph.property("")
        .bindStorage("${javaClass.name}.selectedVersion")
    var version by versionProperty

    override fun setupUI(builder: Panel) {
        with(builder) {
            row(label) {
                val box = comboBox(versions.sortedDescending().map(SemanticVersion::toString)).bindItem(versionProperty)
                val selectedItem = box.component.selectedItem
                if (selectedItem is String) {
                    version = selectedItem
                }

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
}