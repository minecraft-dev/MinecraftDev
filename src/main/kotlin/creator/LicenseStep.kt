/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

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
