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
        val previousBuildSystem = buildSystemBox.selectedItem
        buildSystemBox.removeAllItems()
        buildSystemBox.isEnabled = true

        val creatorConfig = creator.config ?: return

        val types = BuildSystemType.values().filter { type ->
            type.creatorType.isInstance(creatorConfig)
        }

        for (type in types) {
            buildSystemBox.addItem(type)
        }

        if (buildSystemBox.itemCount == 1) {
            buildSystemBox.isEnabled = false
            return
        }

        if (previousBuildSystem != null) {
            buildSystemBox.selectedItem = previousBuildSystem
            return
        }

        buildSystemBox.selectedIndex = 0

        creatorConfig.preferredBuildSystem?.let { buildSystemBox.selectedItem = it }
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
