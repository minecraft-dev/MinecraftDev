/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.exception.EmptyFieldSetupException
import com.demonwav.mcdev.creator.exception.OtherSetupException
import com.demonwav.mcdev.creator.exception.SetupException
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
    private lateinit var buildSystemBox: JComboBox<BuildSystemType>

    override fun getComponent() = panel

    override fun updateStep() {
        buildSystemBox.removeAllItems()
        buildSystemBox.isEnabled = true

        val types = BuildSystemType.values().filter { type ->
            creator.configs.all { type.creatorType.isInstance(it) }
        }

        for (type in types) {
            buildSystemBox.addItem(type)
        }

        buildSystemBox.selectedIndex = 0
        if (buildSystemBox.itemCount == 1) {
            buildSystemBox.isEnabled = false
            return
        }

        // We prefer Gradle, so if it's included, choose it
        // If Gradle is not included, luck of the draw
        if (creator.configs.any { it.preferredBuildSystem == BuildSystemType.GRADLE }) {
            buildSystemBox.selectedItem = BuildSystemType.GRADLE
            return
        }

        val counts = creator.configs.asSequence()
            .mapNotNull { it.preferredBuildSystem }
            .groupingBy { it }
            .eachCount()

        val maxValue = counts.maxBy { it.value }?.value ?: return
        counts.asSequence()
            .filter { it.value == maxValue }
            .map { it.key }
            .firstOrNull()
            ?.let { mostPopularType ->
                buildSystemBox.selectedItem = mostPopularType
            }
    }

    override fun updateDataModel() {
        creator.buildSystem = createBuildSystem()
    }

    private fun createBuildSystem(): BuildSystem {
        val type = buildSystemBox.selectedItem as? BuildSystemType
            ?: throw IllegalStateException("Selected item is not a ${BuildSystemType::class.java.name}")

        return type.create(groupIdField.text, artifactIdField.text, versionField.text)
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
        val NO_WHITESPACE = Regex("\\S+")
    }
}
