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
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStepBase
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem

abstract class AbstractSelectVersionThenForkStep(parent: NewProjectWizardStep, private val versions: List<SemanticVersion>) : AbstractNewProjectWizardMultiStepBase(parent) {
    override fun setupSwitcherUi(builder: Row) {
        with(builder) {
            val box = comboBox(versions.sortedDescending().map(SemanticVersion::toString)).bindItem(stepProperty)
            val selectedItem = box.component.selectedItem
            if (selectedItem is String) {
                step = selectedItem
            }

            // fix the selection to the latest version if it was previously at the latest version
            val props = PropertiesComponent.getInstance()
            val latestVersionProp = "${javaClass.name}.latestVersion"
            val prevLatestVersion = props.getValue(latestVersionProp)
            val latestVersion = versions.maxOrNull()?.toString()
            if (step == prevLatestVersion) {
                step = latestVersion ?: ""
            }
            props.setValue(latestVersionProp, latestVersion)
        }
    }

    override fun initSteps() = versions.associate { it.toString() to initStep(it) }

    abstract fun initStep(version: SemanticVersion) : NewProjectWizardStep
}