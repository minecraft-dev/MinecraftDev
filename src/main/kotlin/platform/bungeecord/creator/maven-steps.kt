/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.addMavenGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractPatchPomStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.MavenImportStep
import com.demonwav.mcdev.creator.buildsystem.ReformatPomStep
import com.demonwav.mcdev.creator.buildsystem.addDefaultMavenProperties
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.SimpleMcVersionStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlTag
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel

class BungeeMavenSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> BungeeMavenFilesStep(parent).chain(::BungeePatchPomStep)
            BuildSystemSupport.POST_STEP -> MavenImportStep(parent).chain(::ReformatPomStep)
            else -> EmptyStep(parent)
        }
    }
}

class BungeeMavenFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven files"

    override fun setupAssets(project: Project) {
        data.putUserData(BungeeProjectFilesStep.VERSION_REF_KEY, "\${project.version}")
        assets.addDefaultMavenProperties()
        assets.addTemplates(project, "pom.xml" to MinecraftTemplates.BUNGEECORD_POM_TEMPLATE)
        if (gitEnabled) {
            assets.addMavenGitignore(project)
        }
    }
}

class BungeePatchPomStep(parent: NewProjectWizardStep) : AbstractPatchPomStep(parent) {
    override fun patchPom(model: MavenDomProjectModel, root: XmlTag) {
        super.patchPom(model, root)
        val platform = data.getUserData(AbstractBungeePlatformStep.KEY) ?: return
        val mcVersion = data.getUserData(SimpleMcVersionStep.KEY) ?: return
        val repositories = platform.getRepositories(mcVersion)
        val dependencies = platform.getDependencies(mcVersion)
        setupDependencies(model, repositories, dependencies)
    }
}
