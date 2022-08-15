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

import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.VersionRange
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.ui.SortedComboBoxModel
import com.intellij.ui.components.Label
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.RowBuilder
import com.intellij.ui.layout.panel
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import org.gradle.util.GradleVersion

class ProjectSetupFinalizerWizardStep(
    val creator: MinecraftProjectCreator,
    val context: WizardContext
) : ModuleWizardStep() {

    private val finalizers: List<ProjectSetupFinalizer> =
        listOf(JdkProjectSetupFinalizer(), GradleProjectSetupFinalizer())
    private val finalizersWithRow: MutableMap<ProjectSetupFinalizer, Row> = linkedMapOf()
    private val applicableFinalizers: MutableSet<ProjectSetupFinalizer> = linkedSetOf()

    private val panel by lazy {
        panel {
            row(Label("<html><font size=\"6\">Project finalization</size></html>")) {}
            finalizers.forEach { finalizer ->
                val row = titledRow("<html><font size=\"5\">${finalizer.title}</size></html>") {
                    with(finalizer) {
                        buildComponent(creator, context)
                    }
                }
                finalizersWithRow[finalizer] = row
            }
        }
    }

    override fun isStepVisible(): Boolean = true

    override fun getComponent(): JComponent = panel

    override fun updateStep() {
        applicableFinalizers.clear()
        for ((finalizer, row) in finalizersWithRow) {
            if (finalizer.isApplicable(creator, context)) {
                applicableFinalizers.add(finalizer)
                finalizer.validateConfigs(creator, context)
                row.visible = true
            } else {
                row.visible = false
            }
        }
    }

    override fun updateDataModel(): Unit = applicableFinalizers.forEach { it.apply(creator, context) }

    override fun validate(): Boolean = applicableFinalizers.all { it.validateChanges(creator, context) }
}

/**
 * Used to adjust project configurations before project creation begins, or simply display a summary.
 * Can also block project creation if problems are found with the configurations (such as version incompatibilities.)
 */
interface ProjectSetupFinalizer {

    val title: String

    /**
     * Builds the component to display in a titled row ([title])
     */
    fun RowBuilder.buildComponent(creator: MinecraftProjectCreator, context: WizardContext)

    /**
     * Whether this finalizer makes sense to appear in the given context.
     *
     * If `false` is returned the component of this finalizer will not be shown, and [validateConfigs],
     * [validateChanges] and [apply] won't be called until it returns `true`.
     *
     * @return `true` if this finalizer applies to the given context, `false` otherwise
     */
    fun isApplicable(creator: MinecraftProjectCreator, context: WizardContext): Boolean

    /**
     * Validates the existing [ProjectConfig]s of this wizard. You can also initialize
     *
     * Finalizers are expected to display errors in their own component.
     *
     * @return `true` if the project setup is valid, `false` otherwise.
     */
    fun validateConfigs(creator: MinecraftProjectCreator, context: WizardContext): Boolean

    /**
     * Validates the changes made in this finalizer's component.
     *
     * @return `true` if the changes are valid, `false` otherwise.
     */
    fun validateChanges(creator: MinecraftProjectCreator, context: WizardContext): Boolean

    /**
     * Applies the changes validated in [validateChanges] to the project configuration.
     */
    fun apply(creator: MinecraftProjectCreator, context: WizardContext)
}

class JdkProjectSetupFinalizer : ProjectSetupFinalizer {

    private val errorLabel = Label("", fontColor = UIUtil.FontColor.BRIGHTER)
        .apply {
            icon = UIUtil.getErrorIcon()
            isVisible = false
        }
    private val sdksModel = ProjectSdksModel()
    private lateinit var jdkBox: JdkComboBox
    private var minimumVersion: JavaSdkVersion = JavaSdkVersion.JDK_1_8

    private fun highestJDKVersionRequired(creator: MinecraftProjectCreator): JavaSdkVersion? {
        val javaVersionRequired = creator.config?.javaVersion ?: return null
        return JavaSdkVersion.fromJavaVersion(javaVersionRequired).also {
            minimumVersion = it ?: JavaSdkVersion.JDK_1_8
        }
    }

    private fun isUsingCompatibleJdk(creator: MinecraftProjectCreator, sdk: Sdk): Boolean {
        val requiredJdkVersion = highestJDKVersionRequired(creator) ?: return false
        return JavaSdk.getInstance().isOfVersionOrHigher(sdk, requiredJdkVersion)
    }

    override val title: String = "JDK"

    override fun RowBuilder.buildComponent(creator: MinecraftProjectCreator, context: WizardContext) {
        row(errorLabel) {}
        jdkBox = JdkComboBox(
            context.project,
            sdksModel,
            { it is JavaSdk },
            { JavaSdk.getInstance().isOfVersionOrHigher(it, minimumVersion) },
            null,
            null,
        )
        reloadJdkBox(context)
        if (jdkBox.itemCount > 0) {
            jdkBox.selectedIndex = 0
        }
        row("JDK version:") {
            component(jdkBox).constraints(grow)
        }
    }

    override fun isApplicable(creator: MinecraftProjectCreator, context: WizardContext) = true

    private fun reloadJdkBox(context: WizardContext) {
        sdksModel.syncSdks()
        sdksModel.reset(context.project)
        jdkBox.reloadModel()
    }

    private fun updateUi(usingCompatibleJdk: Boolean) {
        if (usingCompatibleJdk) {
            errorLabel.text = ""
            errorLabel.isVisible = false
            return
        }

        errorLabel.text = "Project requires at least Java ${minimumVersion.description}"
        errorLabel.isVisible = true
    }

    override fun validateConfigs(creator: MinecraftProjectCreator, context: WizardContext): Boolean {
        val projectJdk = context.projectJdk ?: return true
        val usingCompatibleJdk = isUsingCompatibleJdk(creator, projectJdk)
        updateUi(usingCompatibleJdk)
        return usingCompatibleJdk
    }

    override fun validateChanges(creator: MinecraftProjectCreator, context: WizardContext): Boolean {
        return isUsingCompatibleJdk(creator, jdkBox.selectedJdk ?: return false)
    }

    override fun apply(creator: MinecraftProjectCreator, context: WizardContext) {
        val selectedJdk = jdkBox.selectedJdk
        if (selectedJdk != null) {
            context.projectJdk = selectedJdk
        }
    }
}

class GradleProjectSetupFinalizer : ProjectSetupFinalizer {

    private val model = SortedComboBoxModel<SemanticVersion>(Comparator.naturalOrder())

    private val propertyGraph = PropertyGraph("GradleProjectSetupFinalizer graph")
    var gradleVersion: SemanticVersion by propertyGraph.graphProperty { SemanticVersion.release() }
    private var config: ProjectConfig? = null

    private var gradleVersionRange: VersionRange? = null

    override val title: String = "Gradle"

    override fun RowBuilder.buildComponent(creator: MinecraftProjectCreator, context: WizardContext) {
        row("Gradle version:") {
            comboBox(model, ::gradleVersion)
                .enabled(false) // TODO load compatible Gradle versions list
        }
    }

    override fun isApplicable(creator: MinecraftProjectCreator, context: WizardContext): Boolean {
        val buildSystem = creator.buildSystem
        return buildSystem is GradleBuildSystem
    }

    override fun validateConfigs(creator: MinecraftProjectCreator, context: WizardContext): Boolean {
        config = creator.config

        if (creator.buildSystem !is GradleBuildSystem) {
            return true
        }

        val range = (creator.config as? GradleCreator)?.compatibleGradleVersions
        gradleVersionRange = range

        gradleVersion = range?.upper ?: SemanticVersion.parse(GradleVersion.current().version)
        model.clear()
        model.add(gradleVersion)
        model.selectedItem = gradleVersion
        return true
    }

    override fun validateChanges(creator: MinecraftProjectCreator, context: WizardContext): Boolean {
        if (creator.buildSystem !is GradleBuildSystem) {
            return true
        }
        return gradleVersionRange != null && gradleVersion.parts.isNotEmpty()
    }

    override fun apply(creator: MinecraftProjectCreator, context: WizardContext) {
        (creator.buildSystem as? GradleBuildSystem)?.gradleVersion = gradleVersion
    }
}
