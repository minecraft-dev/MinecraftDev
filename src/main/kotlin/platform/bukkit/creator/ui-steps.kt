/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.creator

import com.demonwav.mcdev.creator.PlatformVersion
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.creator.chain
import com.demonwav.mcdev.creator.getVersionSelector
import com.demonwav.mcdev.creator.platformtype.PluginPlatformStep
import com.demonwav.mcdev.creator.step.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.step.AbstractLatentStep
import com.demonwav.mcdev.creator.step.AbstractOptionalStringStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DependStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.PluginNameStep
import com.demonwav.mcdev.creator.step.SimpleMcVersionStep
import com.demonwav.mcdev.creator.step.SoftDependStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStep
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.coroutineScope

class BukkitPlatformStep(
    parent: PluginPlatformStep
) : AbstractNewProjectWizardMultiStep<BukkitPlatformStep, BukkitPlatformStep.Factory>(parent, EP_NAME) {
    companion object {
        val EP_NAME = ExtensionPointName<Factory>("com.demonwav.minecraft-dev.bukkitPlatformWizard")
    }

    override val self = this
    override val label = "Bukkit Platform:"

    class PlatformFactory : PluginPlatformStep.Factory {
        override val name = "Bukkit"

        override fun createStep(parent: PluginPlatformStep) = BukkitPlatformStep(parent)
    }

    interface Factory : NewProjectWizardMultiStepFactory<BukkitPlatformStep>
}

abstract class AbstractBukkitPlatformStep(
    parent: BukkitPlatformStep,
    private val platform: PlatformType
) : AbstractLatentStep<PlatformVersion>(parent) {
    override val description = "download versions"

    override suspend fun computeData() = coroutineScope {
        try {
            asyncIO { getVersionSelector(platform) }.await()
        } catch (e: Throwable) {
            null
        }
    }

    override fun createStep(data: PlatformVersion) =
        SimpleMcVersionStep(this, data.versions.mapNotNull(SemanticVersion::tryParse)).chain(
            ::PluginNameStep,
            ::MainClassStep,
            ::BukkitOptionalSettingsStep,
            ::BukkitBuildSystemStep,
            ::BukkitProjectFilesStep,
            ::BukkitPostBuildSystemStep,
        )

    override fun setupProject(project: Project) {
        data.putUserData(KEY, this)
        super.setupProject(project)
    }

    abstract fun getRepositories(mcVersion: SemanticVersion): List<BuildRepository>

    abstract fun getDependencies(mcVersion: SemanticVersion): List<BuildDependency>

    companion object {
        val KEY = Key.create<AbstractBukkitPlatformStep>("${AbstractBukkitPlatformStep::class.java.name}.platform")
    }
}

class BukkitOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this).chain(
        ::AuthorsStep,
        ::WebsiteStep,
        ::BukkitLogPrefixStep,
        ::BukkitLoadOrderStep,
        ::BukkitLoadBeforeStep,
        ::DependStep,
        ::SoftDependStep,
    )
}

class BukkitLogPrefixStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Log Prefix:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${BukkitLogPrefixStep::class.java.name}.logPrefix")
    }
}

class BukkitLoadOrderStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val loadOrderProperty = propertyGraph.property(LoadOrder.POSTWORLD)
    private var loadOrder by loadOrderProperty

    init {
        loadOrderProperty.transform(LoadOrder::name, LoadOrder::valueOf)
            .bindStorage("${javaClass.name}.loadOrder")
    }

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("Load at:") {
                segmentedButton(LoadOrder.values().toList(), LoadOrder::toString)
                    .bind(loadOrderProperty)
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, loadOrder)
    }

    companion object {
        val KEY = Key.create<LoadOrder>("${BukkitLoadOrderStep::class.java.name}.loadOrder")
    }
}

class BukkitLoadBeforeStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Load Before:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, AuthorsStep.parseAuthors(value))
    }

    companion object {
        val KEY = Key.create<List<String>>("${BukkitLoadBeforeStep::class.java.name}.loadBefore")
    }
}
