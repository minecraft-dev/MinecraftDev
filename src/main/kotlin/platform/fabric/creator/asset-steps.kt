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

package com.demonwav.mcdev.platform.fabric.creator

import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.step.*
import com.demonwav.mcdev.platform.forge.inspections.sideonly.Side
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_MIXINS_JSON_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_MOD_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_MOD_CLIENT_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FABRIC_MOD_JSON_TEMPLATE
import com.demonwav.mcdev.util.toJavaClassName
import com.demonwav.mcdev.util.toPackageName
import com.intellij.ide.starters.local.GeneratorEmptyDirectory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil

const val MAGIC_DEFERRED_INIT_FILE = ".hello_fabric_from_mcdev"

class FabricBaseFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Adding Fabric project files (phase 1)"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val modId = data.getUserData(AbstractModIdStep.KEY) ?: return
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val packageName = "${buildSystemProps.groupId.toPackageName()}.${modId.toPackageName()}"
        val mainClassName = modName.toJavaClassName()
        val clientClassName = "${modName.toJavaClassName()}Client"
        val mainClass = "$packageName.${modName.toJavaClassName()}"
        val clientClass = "$packageName.client.${modName.toJavaClassName()}Client"
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val envName = when (data.getUserData(FabricEnvironmentStep.KEY) ?: Side.NONE) {
            Side.CLIENT -> "client"
            Side.SERVER -> "server"
            else -> "*"
        }
        val loaderVersion = data.getUserData(FabricVersionChainStep.LOADER_VERSION_KEY) ?: return
        val mcVersion = data.getUserData(FabricVersionChainStep.MC_VERSION_KEY) ?: return
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val apiVersion = data.getUserData(FabricVersionChainStep.API_VERSION_KEY)
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val repo = data.getUserData(RepositoryStep.KEY) ?: ""

        assets.addTemplateProperties(
            "MAIN_PACKAGE" to packageName,
            "CLIENT_PACKAGE" to "${packageName}.client",
            "MAIN_CLASS_NAME" to mainClassName,
            "CLIENT_CLASS_NAME" to clientClassName,
            "MAIN_CLASS" to mainClass,
            "CLIENT_CLASS" to clientClass,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_ID" to modId,
            "MOD_NAME" to StringUtil.escapeStringCharacters(modName),
            "MOD_DESCRIPTION" to StringUtil.escapeStringCharacters(description),
            "MOD_ENVIRONMENT" to envName,
            "LOADER_VERSION" to loaderVersion,
            "MC_VERSION" to mcVersion,
            "JAVA_VERSION" to javaVersion,
            "LICENSE" to license.id,
        )

        if (apiVersion != null) {
            assets.addTemplateProperties("API_VERSION" to apiVersion)
        }

        if (authors.isNotEmpty()) {
            assets.addTemplateProperties("AUTHOR_LIST" to authors.map { s -> "\u0022${s}\u0022" }.joinToString(","))
        }

        if (website.isNotBlank()) {
            assets.addTemplateProperties("WEBSITE" to website)
        }

        if (repo.isNotBlank()) {
            assets.addTemplateProperties("REPOSITORY" to repo)
        }

        if (useMixins) {
            val mixinPackageName = "${buildSystemProps.groupId.toPackageName()}.${modId.toPackageName()}.mixin"
            assets.addTemplateProperties(
                "MIXINS" to "true",
                "MIXIN_PACKAGE_NAME" to mixinPackageName,
            )

            val mixinsJsonFile = "src/main/resources/$modId.mixins.json"
            assets.addTemplates(project, mixinsJsonFile to FABRIC_MIXINS_JSON_TEMPLATE)
        }

        assets.addLicense(project)

        assets.addAssets(
            GeneratorEmptyDirectory("src/main/java"),
            GeneratorEmptyDirectory("src/main/resources"),
        )

        assets.addTemplates(project,
            "src/main/java/${mainClass.replace(".", "/")}.java" to FABRIC_MOD_CLASS_TEMPLATE,
            "src/main/java/${clientClass.replace(".", "/")}.java" to FABRIC_MOD_CLIENT_CLASS_TEMPLATE,
            "src/main/resources/fabric.mod.json" to FABRIC_MOD_JSON_TEMPLATE
        )

        WriteAction.runAndWait<Throwable> {
            val dir = VfsUtil.createDirectoryIfMissing(
                LocalFileSystem.getInstance(),
                "${assets.outputDirectory}/.gradle",
            ) ?: throw IllegalStateException("Unable to create .gradle directory")

            dir.findOrCreateChildData(this, MAGIC_DEFERRED_INIT_FILE)
        }

    }
}

class FabricBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Fabric"
}

class FabricPostBuildSystemStep(
    parent: NewProjectWizardStep,
) : AbstractRunBuildSystemStep(parent, FabricBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}
