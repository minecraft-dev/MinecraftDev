/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.exception.EmptyFieldSetupException
import com.demonwav.mcdev.exception.OtherSetupException
import com.demonwav.mcdev.exception.SetupException
import com.demonwav.mcdev.platform.forge.ForgeProjectConfiguration
import com.demonwav.mcdev.platform.liteloader.LiteLoaderProjectConfiguration
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTextField

class BuildSystemWizardStep(private val creator: MinecraftProjectCreator) : ModuleWizardStep() {

    private lateinit var groupIdField: JTextField
    private lateinit var artifactIdField: JTextField
    private lateinit var versionField: JTextField
    private lateinit var panel: JPanel
    private lateinit var buildSystemBox: JComboBox<String>

    override fun getComponent() = panel

    override fun updateStep() {
        if (creator.configs.size > 1) {
            buildSystemBox.selectedIndex = 1
            buildSystemBox.isVisible = false
            return
        }
        when {
            creator.configs.any { s -> s is ForgeProjectConfiguration || s is LiteLoaderProjectConfiguration } -> {
                buildSystemBox.selectedIndex = 1
                buildSystemBox.setVisible(false)
            }
            creator.configs.any { s -> s is SpongeProjectConfiguration } -> {
                buildSystemBox.selectedIndex = 1
                buildSystemBox.setVisible(true)
            }
            else -> {
                buildSystemBox.selectedIndex = 0
                buildSystemBox.setVisible(true)
            }
        }
    }

    override fun updateDataModel() {}

    override fun onStepLeaving() {
        creator.buildSystem = createBuildSystem()
    }

    private fun createBuildSystem(): BuildSystem {
        return if (buildSystemBox.selectedIndex == 0) {
            MavenBuildSystem(artifactIdField.text, groupIdField.text, versionField.text)
        } else {
            GradleBuildSystem(artifactIdField.text, groupIdField.text, versionField.text)
        }
    }

    override fun validate(): Boolean {
        try {
            if (groupIdField.text.isEmpty()) {
                throw EmptyFieldSetupException(groupIdField)
            }

            if (artifactIdField.text.isEmpty()) {
                throw EmptyFieldSetupException(artifactIdField)
            }

            if (versionField.text.isBlank()) {
                throw EmptyFieldSetupException(versionField)
            }

            if (!groupIdField.text.matches(NO_WHITESPACE)) {
                throw OtherSetupException("The GroupId field cannot contain any whitespace", groupIdField)
            }

            if (!artifactIdField.text.matches(NO_WHITESPACE)) {
                throw OtherSetupException("The ArtifactId field cannot contain any whitespace", artifactIdField)
            }

            if (creator.configs.any { s -> s is ForgeProjectConfiguration } && buildSystemBox.selectedIndex == 0) {
                throw OtherSetupException("Forge does not support Maven", buildSystemBox)
            }
        } catch (e: SetupException) {
            JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(e.error, MessageType.ERROR, null)
                .setFadeoutTime(2000)
                .createBalloon()
                .show(RelativePoint.getSouthWestOf(e.j), Balloon.Position.below)
            return false
        }

        return true
    }

    companion object {
        val NO_WHITESPACE = "\\S+".toRegex()
    }
}
