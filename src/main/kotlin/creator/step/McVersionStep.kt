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
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.storeToData
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.onShown
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel

class SimpleMcVersionStep(
    parent: NewProjectWizardStep,
    versions: List<SemanticVersion>,
) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label
        get() = MCDevBundle("creator.ui.mc_version.label")

    override fun setupUI(builder: Panel) {
        super.setupUI(builder)
        versionProperty.afterChange {
            applyJdkVersion()
        }
        versionBox.onShown {
            applyJdkVersion()
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(version))
        applyJdkVersion()
    }

    private fun applyJdkVersion() {
        val version = SemanticVersion.tryParse(version) ?: return
        findStep<JdkProjectSetupFinalizer>().setPreferredJdk(
            MinecraftVersions.requiredJavaVersion(version),
            "Minecraft $version",
        )
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${SimpleMcVersionStep::class.java.name}.version")
    }
}

abstract class AbstractMcVersionChainStep(
    parent: NewProjectWizardStep,
    vararg otherLabels: String,
) : AbstractVersionChainStep(parent, *(listOf("Minecraft Version:") + otherLabels).toTypedArray()) {
    companion object {
        const val MINECRAFT_VERSION = 0
    }

    init {
        storeToData()
    }

    override fun setupUI(builder: Panel) {
        super.setupUI(builder)
        getVersionProperty(MINECRAFT_VERSION).afterChange {
            applyJdkVersion()
        }
        getVersionBox(MINECRAFT_VERSION)!!.onShown {
            applyJdkVersion()
        }
    }

    override fun setupProject(project: Project) {
        super.setupProject(project)
        applyJdkVersion()
    }

    private fun applyJdkVersion() {
        val version = SemanticVersion.tryParse(getVersion(MINECRAFT_VERSION).toString()) ?: return
        findStep<JdkProjectSetupFinalizer>().setPreferredJdk(
            MinecraftVersions.requiredJavaVersion(version),
            "Minecraft ${getVersion(MINECRAFT_VERSION)}",
        )
    }
}
