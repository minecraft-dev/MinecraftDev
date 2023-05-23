/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addMavenGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractPatchPomStep
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.MavenImportStep
import com.demonwav.mcdev.creator.buildsystem.ReformatPomStep
import com.demonwav.mcdev.creator.buildsystem.addDefaultMavenProperties
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DependStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlTag
import java.util.EnumSet
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel

class SpongeMavenSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> SpongeMavenFilesStep(parent).nextStep(::SpongePatchPomStep)
            BuildSystemSupport.POST_STEP -> SpongeMavenProjectFilesStep(parent)
                .nextStep(::MavenImportStep)
                .nextStep(::ReformatPomStep)
            else -> EmptyStep(parent)
        }
    }
}

class SpongeMavenFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven files"

    override fun setupAssets(project: Project) {
        assets.addDefaultMavenProperties()
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        assets.addTemplateProperties("JAVA_VERSION" to javaVersion)
        assets.addTemplates(project, "pom.xml" to MinecraftTemplates.SPONGE_POM_TEMPLATE)
        if (gitEnabled) {
            assets.addMavenGitignore(project)
        }
    }
}

class SpongeMavenProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven project files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val spongeApiVersion = data.getUserData(SpongeApiVersionStep.KEY) ?: return
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val dependencies = data.getUserData(DependStep.KEY) ?: emptyList()

        assets.addTemplateProperties(
            "PLUGIN_ID" to buildSystemProps.artifactId,
            "VERSION_PLACEHOLDER" to "\${version}",
            "SPONGEAPI_VERSION" to spongeApiVersion,
            "LICENSE" to license.id,
            "PLUGIN_NAME" to pluginName,
            "MAIN_CLASS" to mainClass,
            "DESCRIPTION" to description,
            "WEBSITE" to website,
            "AUTHORS" to authors,
            "DEPENDENCIES" to dependencies,
        )
        assets.addTemplates(
            project,
            "src/main/resources/META-INF/sponge_plugins.json" to MinecraftTemplates.SPONGE8_PLUGINS_JSON_TEMPLATE,
        )
        assets.addLicense(project)
    }
}

class SpongePatchPomStep(parent: NewProjectWizardStep) : AbstractPatchPomStep(parent) {
    override fun patchPom(model: MavenDomProjectModel, root: XmlTag) {
        super.patchPom(model, root)
        val spongeApiVersion = data.getUserData(SpongeApiVersionStep.KEY) ?: return
        setupDependencies(
            model,
            listOf(
                BuildRepository(
                    "spongepowered-repo",
                    "https://repo.spongepowered.org/maven/",
                    buildSystems = EnumSet.of(BuildSystemType.MAVEN),
                ),
            ),
            listOf(
                BuildDependency(
                    "org.spongepowered",
                    "spongeapi",
                    spongeApiVersion.toString(),
                    mavenScope = "provided",
                ),
            ),
        )
    }
}
