/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.VersionRange
import com.demonwav.mcdev.util.until
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.MultiLineLabelUI
import com.intellij.ui.SortedComboBoxModel
import com.intellij.ui.components.Label
import com.intellij.ui.layout.RowBuilder
import com.intellij.ui.layout.panel
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent

class ProjectSetupFinalizerWizardStep(
    val creator: MinecraftProjectCreator,
    val context: WizardContext
) : ModuleWizardStep() {

    private val validators: List<ProjectSetupFinalizer> =
        listOf(JdkProjectSetupFinalizer(), GradleProjectSetupFinalizer())

    override fun isStepVisible(): Boolean = validators.any { !it.validateConfigs(creator, context) }

    override fun getComponent(): JComponent = panel {
        row(Label("<html><font size=\"6\">Project finalization</size></html>")) {}
        validators.forEach { validator ->
            titledRow("<html><font size=\"5\">${validator.title}</size></html>") {
                with(validator) {
                    buildComponent(creator, context)
                }
            }
        }
    }

    override fun updateStep() {
        for (validator in validators) {
            validator.validateConfigs(creator, context)
        }
    }

    override fun updateDataModel(): Unit = validators.forEach { it.apply(creator, context) }

    override fun validate(): Boolean = validators.all { it.validateChanges(creator, context) }
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
        .apply { icon = UIUtil.getErrorIcon() }
    private val sdksModel = ProjectSdksModel()
    private lateinit var jdkBox: JdkComboBox
    private var minimumVersion: JavaSdkVersion = JavaSdkVersion.JDK_1_8

    private fun highestJDKVersionRequired(creator: MinecraftProjectCreator): JavaSdkVersion? {
        val highestJavaVersionRequired = creator.configs.maxOfOrNull { it.javaVersion } ?: return null
        return JavaSdkVersion.fromJavaVersion(highestJavaVersionRequired).also {
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

    private val errorLabel = Label("", fontColor = UIUtil.FontColor.BRIGHTER)
        .apply {
            icon = UIUtil.getErrorIcon()
            setUI(MultiLineLabelUI())
        }
    private val model = SortedComboBoxModel<SemanticVersion>(Comparator.naturalOrder())

    private val propertyGraph = PropertyGraph("GradleProjectSetupValidator graph")
    var gradleVersion: SemanticVersion by propertyGraph.graphProperty { SemanticVersion.release() }
    private var configs: Collection<ProjectConfig> = emptyList()
    private var incompatibleConfigs: List<ProjectConfig> = emptyList()

    private var gradleVersionRange: VersionRange? = null

    override val title: String = "Gradle"

    override fun RowBuilder.buildComponent(creator: MinecraftProjectCreator, context: WizardContext) {
        row(errorLabel) {}
        row("Gradle version:") {
            comboBox(model, ::gradleVersion)
                .enabled(false) // TODO load compatible Gradle versions list
        }
    }

    override fun validateConfigs(creator: MinecraftProjectCreator, context: WizardContext): Boolean {
        configs = creator.configs
        incompatibleConfigs = emptyList()

        if (creator.buildSystem !is GradleBuildSystem) {
            updateUi()
            return true
        }

        val incompatibleConfigs = mutableListOf<ProjectConfig>()
        val range = creator.configs.fold(SemanticVersion.release() until null) { acc, config ->
            val range = (config as? GradleCreator)?.compatibleGradleVersions ?: return@fold acc
            val intersection = acc.intersect(range)
            if (intersection == null) {
                incompatibleConfigs.add(config)
                return@fold acc
            }
            intersection
        }
        gradleVersionRange = range
        this.incompatibleConfigs = incompatibleConfigs
        updateUi()

        // TODO get the compatible versions available if possible
        gradleVersion = range.lower
        model.clear()
        model.add(gradleVersion)
        model.selectedItem = gradleVersion
        return this.incompatibleConfigs.isEmpty()
    }

    private fun updateUi() {
        if (incompatibleConfigs.isEmpty()) {
            errorLabel.text = ""
            errorLabel.isVisible = false
            return
        }

        val problemsList = incompatibleConfigs.joinToString(separator = "") { config ->
            val configName = config.javaClass.simpleName.removeSuffix("ProjectConfig")
            val compatibleGradleVersions = (config as GradleCreator).compatibleGradleVersions
            "\n- $configName requires $compatibleGradleVersions"
        }
        val compatibleConfigsList = configs.subtract(incompatibleConfigs)
            .joinToString { it.javaClass.simpleName.removeSuffix("ProjectConfig") }
        errorLabel.text = "$compatibleConfigsList require Gradle $gradleVersionRange but:$problemsList"
        errorLabel.isVisible = true
    }

    override fun validateChanges(creator: MinecraftProjectCreator, context: WizardContext): Boolean {
        if (creator.buildSystem !is GradleBuildSystem) {
            return true
        }
        return gradleVersionRange != null && incompatibleConfigs.isEmpty() && gradleVersion.parts.isNotEmpty()
    }

    override fun apply(creator: MinecraftProjectCreator, context: WizardContext) {
        (creator.buildSystem as? GradleBuildSystem)?.gradleVersion = gradleVersion
    }
}
