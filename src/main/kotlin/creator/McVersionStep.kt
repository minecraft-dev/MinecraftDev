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

import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.onShown
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel

class SimpleMcVersionStep(
    parent: NewProjectWizardStep,
    versions: List<SemanticVersion>
) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Minecraft Version:"

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
            "Minecraft $version"
        )
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${SimpleMcVersionStep::class.java.name}.version")
    }
}

abstract class AbstractSelectMcVersionThenForkStep<T : Comparable<T>>(
    parent: NewProjectWizardStep,
    versions: List<T>
) : AbstractSelectVersionThenForkStep<T>(parent, versions) {
    override fun setupUI(builder: Panel) {
        super.setupUI(builder)
        stepProperty.afterChange {
            applyJdkVersion()
        }
        versionBox.onShown {
            applyJdkVersion()
        }
    }

    override fun setupProject(project: Project) {
        super.setupProject(project)
        applyJdkVersion()
    }

    private fun applyJdkVersion() {
        val version = SemanticVersion.tryParse(step) ?: return
        findStep<JdkProjectSetupFinalizer>().setPreferredJdk(
            MinecraftVersions.requiredJavaVersion(version),
            "Minecraft $step"
        )
    }
}
