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

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.platformtype.PlatformTypeStep
import com.demonwav.mcdev.platform.MinecraftModuleType
import com.demonwav.mcdev.platform.architectury.creator.ArchitecturyProjectSettingsWizard
import com.demonwav.mcdev.platform.bukkit.creator.BukkitProjectSettingsWizard
import com.demonwav.mcdev.platform.bungeecord.creator.BungeeCordProjectSettingsWizard
import com.demonwav.mcdev.platform.fabric.creator.FabricProjectSettingsWizard
import com.demonwav.mcdev.platform.forge.creator.ForgeProjectSettingsWizard
import com.demonwav.mcdev.platform.liteloader.creator.LiteLoaderProjectSettingsWizard
import com.demonwav.mcdev.platform.sponge.creator.SpongeProjectSettingsWizard
import com.demonwav.mcdev.platform.velocity.creator.VelocityProjectSettingsWizard
import com.intellij.ide.projectWizard.ProjectSettingsStep
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareRunnable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MinecraftModuleBuilder : AbstractNewProjectWizardBuilder() {

    private val creator = MinecraftProjectCreator()

    override fun getPresentableName() = MinecraftModuleType.NAME
    override fun getNodeIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getGroupName() = MinecraftModuleType.NAME
    override fun getBuilderId() = "MINECRAFT_MODULE"
    override fun getDescription() = "Create a new Minecraft project"

//    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
//        val project = modifiableRootModel.project
//        val (root, vFile) = createAndGetRoot()
//        modifiableRootModel.addContentEntry(vFile)
//
//        if (moduleJdk != null) {
//            modifiableRootModel.sdk = moduleJdk
//        } else {
//            modifiableRootModel.inheritSdk()
//        }
//
//        val r = DumbAwareRunnable {
//            creator.create(root, modifiableRootModel.module)
//        }
//
//        if (project.isDisposed) {
//            return
//        }
//
//        if (
//            ApplicationManager.getApplication().isUnitTestMode ||
//            ApplicationManager.getApplication().isHeadlessEnvironment
//        ) {
//            r.run()
//            return
//        }
//
//        if (!project.isInitialized) {
//            StartupManager.getInstance(project).registerPostStartupActivity(r)
//            return
//        }
//
//        DumbService.getInstance(project).runWhenSmart(r)
//    }

    private fun createAndGetRoot(): Pair<Path, VirtualFile> {
        val temp = contentEntryPath ?: throw IllegalStateException("Failed to get content entry path")

        val pathName = FileUtil.toSystemIndependentName(temp)

        val path = Paths.get(pathName)
        Files.createDirectories(path)
        val vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(pathName)
            ?: throw IllegalStateException("Failed to refresh and file file: $path")

        return path to vFile
    }

    override fun getParentGroup() = MinecraftModuleType.NAME
    override fun createStep(context: WizardContext) = RootNewProjectWizardStep(context)
        .chain(::NewProjectWizardBaseStep, ::PlatformTypeStep, ::BuildSystemPropertiesStep, ::ProjectSetupFinalizerWizardStep)

    override fun getIgnoredSteps() = listOf(ProjectSettingsStep::class.java)
}
