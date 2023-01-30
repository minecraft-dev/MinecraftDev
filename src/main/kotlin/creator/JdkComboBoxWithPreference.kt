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

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.projectWizard.ProjectWizardUtil
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractWizard
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkListItem
import com.intellij.openapi.roots.ui.configuration.SdkListModel
import com.intellij.openapi.roots.ui.configuration.SdkListModelBuilder.ModelListener
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.roots.ui.configuration.validateSdk
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import javax.swing.JComponent

internal class JdkPreferenceData(
    var jdk: JavaSdkVersion,
    val sdkPathByJdk: MutableMap<JavaSdkVersion, String>,
    var ignoreChangesForPreference: Boolean,
)

class JdkComboBoxWithPreference internal constructor(
    project: Project?,
    private val model: ProjectSdksModel,
    sdkFilter: Condition<in Sdk>?,
    private val preferenceData: JdkPreferenceData,
) : JdkComboBox(project, model, null, sdkFilter, null, null, null) {
    private var suggestions = emptyList<SdkListItem.SuggestedItem>()

    init {
        myModel.addModelListener(object : ModelListener {
            override fun syncModel(model: SdkListModel) {
                suggestions = model.items.filterIsInstance<SdkListItem.SuggestedItem>()
            }
        })
    }

    internal fun loadSuggestions(windowChild: JComponent, disposable: Disposable) {
        myModel.detectItems(windowChild, disposable)
    }

    internal fun getTargetJdk(project: Project?): Sdk? {
        selectedJdk?.let { return it }

        return if (project != null && isProjectJdkSelected) {
            ProjectRootManager.getInstance(project).projectSdk
        } else {
            null
        }
    }

    fun setPreferredJdk(version: JavaSdkVersion) {
        if (version == preferenceData.jdk) {
            return
        }

        preferenceData.ignoreChangesForPreference = true
        try {
            preferenceData.jdk = version
            reloadModel()

            for (jdkVersion in version.ordinal until JavaSdkVersion.values().size) {
                val jdk = JavaSdkVersion.values()[jdkVersion]

                val preferredSdkPath = preferenceData.sdkPathByJdk[jdk]
                if (preferredSdkPath != null) {
                    val sdk = model.sdks.firstOrNull { it.homePath == preferredSdkPath }
                        ?: suggestions.firstOrNull { it.homePath == preferredSdkPath }
                    if (sdk != null) {
                        setSelectedItem(sdk)
                        return
                    }
                }

                val sdk = model.sdks.firstOrNull { JavaSdk.getInstance().getVersion(it) == jdk }
                if (sdk != null) {
                    setSelectedItem(sdk)
                    return
                }
            }
        } finally {
            preferenceData.ignoreChangesForPreference = false
        }
    }
}

fun Row.jdkComboBoxWithPreference(
    context: WizardContext,
    sdkProperty: ObservableMutableProperty<Sdk?>,
    sdkPropertyId: String
): Cell<JdkComboBoxWithPreference> {
    val sdkModel = ProjectSdksModel()

    Disposer.register(context.disposable) {
        sdkModel.disposeUIResources()
    }

    val project = context.project
    sdkModel.reset(project)

    val preferenceData = JdkPreferenceData(JavaSdkVersion.JDK_17, mutableMapOf(), false)

    val sdkFilter = Condition<Sdk> {
        val version = it.versionString?.let(JavaSdkVersion::fromVersionString)
            ?: return@Condition true
        version >= preferenceData.jdk
    }
    val comboBox = JdkComboBoxWithPreference(context.project, sdkModel, sdkFilter, preferenceData)

    val selectedJdkProperty = "jdk.selected.$sdkPropertyId"
    val preferenceDataProperty = "jdk.preference.$sdkPropertyId"
    val stateComponent = project?.let(PropertiesComponent::getInstance) ?: PropertiesComponent.getInstance()

    stateComponent.getList(preferenceDataProperty)?.let { preferenceDataStrs ->
        for (preferenceDataStr in preferenceDataStrs) {
            val parts = preferenceDataStr.split('=', limit = 2)
            val jdk = parts.firstOrNull()?.toIntOrNull()?.let { JavaSdkVersion.values()[it] } ?: continue
            val sdk = parts.last()
            preferenceData.sdkPathByJdk[jdk] = sdk
        }
    }

    comboBox.addActionListener {
        val sdk = comboBox.getTargetJdk(project)
        if (sdk != null) {
            stateComponent.setValue(selectedJdkProperty, sdk.name)

            if (!preferenceData.ignoreChangesForPreference) {
                val jdk = JavaSdk.getInstance().getVersion(sdk)
                val homePath = sdk.homePath
                if (jdk != null && homePath != null) {
                    preferenceData.sdkPathByJdk[jdk] = homePath
                    stateComponent.setList(
                        preferenceDataProperty,
                        preferenceData.sdkPathByJdk.map { (jdk, sdk) -> "${jdk.ordinal}=$sdk" }
                    )
                }
            }
        }
        sdkProperty.set(sdk)
    }

    val lastUsedSdk = stateComponent.getValue(selectedJdkProperty)
    ProjectWizardUtil.preselectJdkForNewModule(project, lastUsedSdk, comboBox) { true }

    val windowChild = context.getUserData(AbstractWizard.KEY)!!.contentPanel
    comboBox.loadSuggestions(windowChild, context.disposable)

    return cell(comboBox)
        .validationOnApply { validateSdk(sdkProperty, sdkModel) }
        .onApply { context.projectJdk = sdkProperty.get() }
}
