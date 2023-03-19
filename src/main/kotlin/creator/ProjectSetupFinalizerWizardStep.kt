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

import com.demonwav.mcdev.creator.ProjectSetupFinalizer.Factory
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.ui.validation.WHEN_GRAPH_PROPAGATION_FINISHED
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Placeholder
import javax.swing.JLabel
import javax.swing.JPanel

class ProjectSetupFinalizerWizardStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val finalizers: List<ProjectSetupFinalizer> by lazy {
        val factories = ProjectSetupFinalizer.EP_NAME.extensionList
        val result = mutableListOf<ProjectSetupFinalizer>()
        if (factories.isNotEmpty()) {
            var par: NewProjectWizardStep = this
            for (factory in factories) {
                val finalizer = factory.create(par)
                result += finalizer
                par = finalizer
            }
        }
        result
    }
    private val step by lazy {
        when (finalizers.size) {
            0 -> null
            1 -> finalizers[0]
            else -> {
                var step = finalizers[0].nextStep { finalizers[1] }
                for (i in 2 until finalizers.size) {
                    step = step.nextStep { finalizers[i] }
                }
                step
            }
        }
    }

    override fun setupUI(builder: Panel) {
        for (step in finalizers) {
            step.setupUI(builder)
        }
        if (finalizers.isNotEmpty()) {
            builder.row {
                cell(JPanel())
                    .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(propertyGraph))
                    .validation(
                        DialogValidation {
                            finalizers.mapFirstNotNull(ProjectSetupFinalizer::validate)?.let(::ValidationInfo)
                        }
                    )
            }
        }
    }

    override fun setupProject(project: Project) {
        for (step in finalizers) {
            step.setupProject(project)
        }
    }
}

/**
 * A step applied after all other steps for all Minecraft project creators. These steps can also block project creation
 * by providing extra validations.
 *
 * To add custom project setup finalizers, register a [Factory] to the
 * `com.demonwav.minecraft-dev.projectSetupFinalizer` extension point.
 */
interface ProjectSetupFinalizer : NewProjectWizardStep {
    companion object {
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.projectSetupFinalizer")
    }

    /**
     * Validates the existing settings of this wizard.
     *
     * @return `null` if the settings are valid, or an error message if they are invalid.
     */
    fun validate(): String? = null

    interface Factory {
        fun create(parent: NewProjectWizardStep): ProjectSetupFinalizer
    }
}

class JdkProjectSetupFinalizer(
    parent: NewProjectWizardStep,
) : AbstractNewProjectWizardStep(parent), ProjectSetupFinalizer {
    private val sdkProperty: GraphProperty<Sdk?> = propertyGraph.property(null)
    private var sdk by sdkProperty
    private var sdkComboBox: JdkComboBoxWithPreference? = null
    private var preferredJdkLabel: Placeholder? = null
    private var preferredJdkReason = "these settings"

    var preferredJdk: JavaSdkVersion = JavaSdkVersion.JDK_17
        private set

    fun setPreferredJdk(value: JavaSdkVersion, reason: String) {
        preferredJdk = value
        preferredJdkReason = reason
        sdkComboBox?.setPreferredJdk(value)
        updatePreferredJdkLabel()
    }

    init {
        storeToData()

        sdkProperty.afterChange {
            updatePreferredJdkLabel()
        }
    }

    private fun updatePreferredJdkLabel() {
        val sdk = this.sdk ?: return
        val version = JavaSdk.getInstance().getVersion(sdk) ?: return
        if (version == preferredJdk) {
            preferredJdkLabel?.component = null
        } else {
            preferredJdkLabel?.component =
                JLabel("Java ${preferredJdk.description} is recommended for $preferredJdkReason")
                    .also { it.foreground = JBColor.YELLOW }
        }
    }

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("JDK:") {
                val sdkComboBox = jdkComboBoxWithPreference(context, sdkProperty, "${javaClass.name}.sdk")
                this@JdkProjectSetupFinalizer.sdkComboBox = sdkComboBox.component
                this@JdkProjectSetupFinalizer.preferredJdkLabel = placeholder()
                updatePreferredJdkLabel()
            }
        }
    }

    class Factory : ProjectSetupFinalizer.Factory {
        override fun create(parent: NewProjectWizardStep) = JdkProjectSetupFinalizer(parent)
    }
}
