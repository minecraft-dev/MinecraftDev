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

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStepBase
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem

abstract class AbstractSelectVersionThenForkStep<T : Comparable<T>>(
    parent: NewProjectWizardStep,
    private val versions: List<T>
) : AbstractNewProjectWizardMultiStepBase(parent) {
    protected lateinit var versionBox: ComboBox<String>

    override fun setupSwitcherUi(builder: Row) {
        with(builder) {
            val box = comboBox(versions.sortedDescending().map(Any::toString)).bindItem(stepProperty)
            val selectedItem = box.component.selectedItem
            if (selectedItem is String) {
                step = selectedItem
            }
            versionBox = box.component

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

    abstract fun initStep(version: T): NewProjectWizardStep
}
