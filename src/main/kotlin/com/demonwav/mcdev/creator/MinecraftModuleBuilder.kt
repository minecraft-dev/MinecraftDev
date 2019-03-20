/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.MinecraftModuleType
import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.DumbAwareRunnable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class MinecraftModuleBuilder : JavaModuleBuilder() {

    private val creator = MinecraftProjectCreator()

    override fun getPresentableName() = MinecraftModuleType.NAME
    override fun getNodeIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getGroupName() = MinecraftModuleType.NAME
    override fun getWeight() = JavaModuleBuilder.BUILD_SYSTEM_WEIGHT - 1
    override fun getBuilderId() = "MINECRAFT_MODULE"
    override fun isSuitableSdkType(sdk: SdkTypeId?) = sdk === JavaSdk.getInstance()

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val project = modifiableRootModel.project
        val root = createAndGetRoot() ?: return
        modifiableRootModel.addContentEntry(root)

        if (moduleJdk != null) {
            modifiableRootModel.sdk = moduleJdk
        }


        val r = DumbAwareRunnable {
            creator.create(root, modifiableRootModel.module)
        }

        if (project.isDisposed) {
            return
        }

        if (ApplicationManager.getApplication().isUnitTestMode || ApplicationManager.getApplication().isHeadlessEnvironment) {
            r.run()
            return
        }

        if (!project.isInitialized) {
            StartupManager.getInstance(project).registerPostStartupActivity(r)
            return
        }

        DumbService.getInstance(project).runWhenSmart(r)
    }

    private fun createAndGetRoot(): VirtualFile? {
        val temp = contentEntryPath ?: return null

        val path = FileUtil.toSystemIndependentName(temp)

        return try {
            Files.createDirectories(Paths.get(path))
            LocalFileSystem.getInstance().refreshAndFindFileByPath(path)
        } catch (e: IOException) {
            null
        }
    }

    override fun getModuleType(): ModuleType<*> = JavaModuleType.getModuleType()
    override fun getParentGroup() = MinecraftModuleType.NAME

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        return arrayOf(
            SpongeForgeChooser(creator),
            BuildSystemWizardStep(creator),
            BukkitProjectSettingsWizard(creator),
            SpongeProjectSettingsWizard(creator),
            ForgeProjectSettingsWizard(creator),
            LiteLoaderProjectSettingsWizard(creator),
            BungeeCordProjectSettingsWizard(creator)
        )
    }

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?) = ProjectChooserWizardStep(creator)
    override fun validate(current: Project?, dest: Project?) = true
}
