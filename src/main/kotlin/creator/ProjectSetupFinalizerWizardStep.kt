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

import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.VersionRange
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.stepSequence
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.sdkComboBox
import com.intellij.openapi.ui.validation.AFTER_GRAPH_PROPAGATION
import com.intellij.openapi.ui.validation.validationErrorFor
import com.intellij.ui.SortedComboBoxModel
import com.intellij.ui.dsl.builder.Panel
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
                    .validation(validationErrorFor<JPanel> {
                        finalizers.mapFirstNotNull(ProjectSetupFinalizer::validate)
                    })
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

class JdkProjectSetupFinalizer(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent), ProjectSetupFinalizer {
    private val sdkProperty: GraphProperty<Sdk?> = propertyGraph.property(null)
    private var sdk by sdkProperty
    private var sdkComboBox: JdkComboBox? = null

    private var theMinVersion = JavaSdkVersion.JDK_17
    var minVersion
        get() = theMinVersion
        set(version) {
            if (version != theMinVersion) {
                theMinVersion = version
                sdkComboBox?.reloadModel()
            }
        }

    init {
        storeToData()
    }

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("JDK Version:") {
                val sdkComboBox = sdkComboBox(context, sdkProperty, "${javaClass.name}.sdk", sdkFilter = sdkFilter@{ sdk ->
                    val version = sdk.versionString?.let(JavaSdkVersion::fromVersionString) ?: return@sdkFilter true
                    version >= minVersion
                })
                this@JdkProjectSetupFinalizer.sdkComboBox = sdkComboBox.component
            }
        }
    }

    override fun setupProject(project: Project) {
        val sdk = sdk
        if (sdk != null) {
            context.projectJdk = sdk
        }
    }

    class Factory : ProjectSetupFinalizer.Factory {
        override fun create(parent: NewProjectWizardStep) = JdkProjectSetupFinalizer(parent)
    }
}

class GradleProjectSetupFinalizer(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent), ProjectSetupFinalizer {

    private val model = SortedComboBoxModel<SemanticVersion>(Comparator.naturalOrder())

    var gradleVersionProperty = propertyGraph.lazyProperty { SemanticVersion.release() }
    var gradleVersion: SemanticVersion by gradleVersionProperty
    private var config: ProjectConfig? = null

    private var gradleVersionRange: VersionRange? = null

//    override fun validate(): String? {
//        config = creator.config
//
//        if (creator.buildSystem !is GradleBuildSystem) {
//            return true
//        }
//
//        val range = (creator.config as? GradleCreator)?.compatibleGradleVersions
//        gradleVersionRange = range
//
//        gradleVersion = range?.upper ?: SemanticVersion.parse(GradleVersion.current().version)
//        model.clear()
//        model.add(gradleVersion)
//        model.selectedItem = gradleVersion
//        return true
//    }
//
//
//    override fun setupProject(project: Project) {
//        (creator.buildSystem as? GradleBuildSystem)?.gradleVersion = gradleVersion
//    }

    class Factory : ProjectSetupFinalizer.Factory {
        override fun create(parent: NewProjectWizardStep) = GradleProjectSetupFinalizer(parent)
    }
}
