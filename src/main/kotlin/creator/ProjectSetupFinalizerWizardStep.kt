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

import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.stepSequence
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.sdkComboBox
import com.intellij.openapi.ui.validation.AFTER_GRAPH_PROPAGATION
import com.intellij.openapi.ui.validation.validationErrorFor
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
        if (finalizers.isEmpty()) {
            null
        } else {
            stepSequence(finalizers[0], *finalizers.asSequence().drop(1).toTypedArray())
        }
    }

    override fun setupUI(builder: Panel) {
        step?.setupUI(builder)
        if (finalizers.isNotEmpty()) {
            builder.row {
                cell(JPanel())
                    .validationRequestor(AFTER_GRAPH_PROPAGATION(propertyGraph))
                    .validation(
                        validationErrorFor<JPanel> {
                            finalizers.mapFirstNotNull(ProjectSetupFinalizer::validate)
                        }
                    )
            }
        }
    }

    override fun setupProject(project: Project) {
        step?.setupProject(project)
    }
}

/**
 * Used to adjust project configurations before project creation begins, or simply display a summary.
 * Can also block project creation if problems are found with the configurations (such as version incompatibilities.)
 */
interface ProjectSetupFinalizer : NewProjectWizardStep {
    companion object {
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.projectSetupFinalizer")
    }

    /**
     * Validates the existing [ProjectConfig]s of this wizard. You can also initialize
     *
     * Finalizers are expected to display errors in their own component.
     *
     * @return `true` if the project setup is valid, `false` otherwise.
     */
    fun validate(): String? = null

    interface Factory {
        fun create(parent: NewProjectWizardStep): ProjectSetupFinalizer
    }
}

class JdkProjectSetupFinalizer(
    parent: NewProjectWizardStep
) : AbstractNewProjectWizardStep(parent), ProjectSetupFinalizer {
    private val sdkProperty: GraphProperty<Sdk?> = propertyGraph.property(null)
    private var sdk by sdkProperty
    private var sdkComboBox: JdkComboBoxWithPreference? = null
    private var preferredJdkLabel: Placeholder? = null

    var preferredJdk: JavaSdkVersion = JavaSdkVersion.JDK_17
        set(value) {
            field = value
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
                JLabel("Selected JDK does not match platform preferred JDK version ${preferredJdk.description}")
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
