/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.bukkit.creator

import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DependStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.SimpleMcVersionStep
import com.demonwav.mcdev.creator.step.SoftDependStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.bukkit.BukkitModuleType
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

class BukkitProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating project files"

    override fun setupAssets(project: Project) {
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val versionRef = data.getUserData(VERSION_REF_KEY) ?: "\${version}"
        val prefix = data.getUserData(BukkitLogPrefixStep.KEY) ?: ""
        val loadOrder = data.getUserData(BukkitLoadOrderStep.KEY) ?: return
        val loadBefore = data.getUserData(BukkitLoadBeforeStep.KEY) ?: emptyList()
        val deps = data.getUserData(DependStep.KEY) ?: emptyList()
        val softDeps = data.getUserData(SoftDependStep.KEY) ?: emptyList()
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val mcVersion = data.getUserData(SimpleMcVersionStep.KEY) ?: return
        val bukkitPlatform = data.getUserData(AbstractBukkitPlatformStep.KEY) ?: return

        val (packageName, className) = splitPackage(mainClass)

        assets.addTemplateProperties(
            "MAIN" to mainClass,
            "VERSION" to versionRef,
            "NAME" to pluginName,
            "PACKAGE" to packageName,
            "CLASS_NAME" to className,
        )

        if (prefix.isNotBlank()) {
            assets.addTemplateProperties("PREFIX" to prefix)
        }

        if (loadOrder != LoadOrder.POSTWORLD) {
            assets.addTemplateProperties("LOAD" to loadOrder.name)
        }

        if (loadBefore.isNotEmpty()) {
            assets.addTemplateProperties("LOAD_BEFORE" to loadBefore)
        }

        if (deps.isNotEmpty()) {
            assets.addTemplateProperties("DEPEND" to deps)
        }

        if (softDeps.isNotEmpty()) {
            assets.addTemplateProperties("SOFT_DEPEND" to softDeps)
        }

        if (authors.isNotEmpty()) {
            assets.addTemplateProperties("AUTHOR_LIST" to authors)
        }

        if (description.isNotBlank()) {
            assets.addTemplateProperties("DESCRIPTION" to description)
        }

        if (website.isNotEmpty()) {
            assets.addTemplateProperties("WEBSITE" to website)
        }

        if (mcVersion >= BukkitModuleType.API_TAG_VERSION) {
            assets.addTemplateProperties("API_VERSION" to mcVersion.take(2))
        }

        val (manifestPath, manifestTemplate) = bukkitPlatform.getManifest()
        assets.addTemplates(
            project,
            manifestPath to manifestTemplate,
            "src/main/java/${mainClass.replace('.', '/')}.java" to MinecraftTemplates.BUKKIT_MAIN_CLASS_TEMPLATE,
        )
    }

    companion object {
        val VERSION_REF_KEY = Key.create<String>("${BukkitProjectFilesStep::class.java.name}.versionRef")
    }
}

class BukkitBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Bukkit"
}

class BukkitPostBuildSystemStep(
    parent: NewProjectWizardStep,
) : AbstractRunBuildSystemStep(parent, BukkitBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}
