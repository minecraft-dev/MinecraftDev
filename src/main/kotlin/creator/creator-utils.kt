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

import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.FixedAssetsNewProjectWizardStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.starters.local.GeneratorTemplateFile
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GitNewProjectWizardData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.RecursionManager
import java.time.ZonedDateTime

var NewProjectWizardStep.gitEnabled
    get() = data.getUserData(GitNewProjectWizardData.KEY)!!.git
    set(value) {
        data.getUserData(GitNewProjectWizardData.KEY)!!.git = value
    }

fun FixedAssetsNewProjectWizardStep.addGradleGitignore(project: Project) {
    addTemplates(project, ".gitignore" to MinecraftTemplates.GRADLE_GITIGNORE_TEMPLATE)
}

fun FixedAssetsNewProjectWizardStep.addMavenGitignore(project: Project) {
    addTemplates(project, ".gitignore" to MinecraftTemplates.MAVEN_GITIGNORE_TEMPLATE)
}

fun FixedAssetsNewProjectWizardStep.addTemplates(project: Project, vararg templates: Pair<String, String>) {
    addTemplates(project, templates.toMap())
}

fun FixedAssetsNewProjectWizardStep.addTemplates(project: Project, templates: Map<String, String>) {
    val manager = FileTemplateManager.getInstance(project)
    addAssets(templates.map { (path, template) -> GeneratorTemplateFile(path, manager.getJ2eeTemplate(template)) })
}

fun FixedAssetsNewProjectWizardStep.addLicense(project: Project) {
    val license = data.getUserData(LicenseStep.KEY) ?: return
    val authors = data.getUserData(AuthorsStep.KEY) ?: return
    addTemplateProperties(
        "YEAR" to ZonedDateTime.now().year,
        "AUTHOR" to authors.joinToString(", "),
    )
    addTemplates(project, "LICENSE" to "${license.id}.txt")
}

fun splitPackage(text: String): Pair<String, String> {
    val index = text.lastIndexOf('.')
    val className = text.substring(index + 1)
    val packageName = text.substring(0, index)
    return packageName to className
}

private val stepClassToKey = mutableMapOf<Class<*>, Key<*>>()

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T : NewProjectWizardStep> getOrCreateClassKey(clazz: Class<out T>) =
    stepClassToKey.computeIfAbsent(clazz) {
        Key.create<T>(it.name)
    } as Key<T>

private val stepClassToWhenAvailableKey = mutableMapOf<Class<*>, Key<*>>()

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T : NewProjectWizardStep> getWhenAvailableKey(clazz: Class<out T>) =
    stepClassToWhenAvailableKey[clazz] as Key<MutableList<(T) -> Unit>>?

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T : NewProjectWizardStep> getOrCreateWhenAvailableKey(clazz: Class<out T>) =
    stepClassToWhenAvailableKey.computeIfAbsent(clazz) {
        Key.create<T>(it.name)
    } as Key<MutableList<(T) -> Unit>>

inline fun <reified T : NewProjectWizardStep> T.storeToData() {
    storeToData(T::class.java)
}

fun <T : NewProjectWizardStep> T.storeToData(clazz: Class<out T>) {
    data.putUserData(getOrCreateClassKey(clazz), this)
    getWhenAvailableKey(clazz)?.let { whenAvailableKey ->
        data.getUserData(whenAvailableKey)?.let { whenAvailable ->
            for (func in whenAvailable) {
                func(this)
            }
            data.putUserData(whenAvailableKey, null)
        }
    }
}

inline fun <reified T : NewProjectWizardStep> NewProjectWizardStep.findStep(): T {
    return findStep(T::class.java)
}

fun <T : NewProjectWizardStep> NewProjectWizardStep.findStep(clazz: Class<out T>): T {
    return data.getUserData(getOrCreateClassKey(clazz))
        ?: throw IllegalStateException("Could not find required step ${clazz.name}")
}

inline fun <reified T : NewProjectWizardStep> NewProjectWizardStep.whenStepAvailable(noinline func: (T) -> Unit) {
    val value = data.getUserData(getOrCreateClassKey(T::class.java))
    if (value != null) {
        func(value)
    } else {
        val whenAvailableKey = getOrCreateWhenAvailableKey(T::class.java)
        val whenAvailable = data.getUserData(whenAvailableKey)
            ?: mutableListOf<(T) -> Unit>().also { data.putUserData(whenAvailableKey, it) }
        whenAvailable += func
    }
}

private val updateWhenChangedGuard =
    RecursionManager.createGuard<ObservableMutableProperty<*>>("mcdev.updateWhenChangedGuard")

fun <T> ObservableMutableProperty<T>.updateWhenChanged(dependency: ObservableProperty<*>, suggestor: () -> T) {
    dependency.afterChange {
        updateWhenChangedGuard.doPreventingRecursion(this, false) {
            set(suggestor())
        }
    }
}

class EmptyStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent)
