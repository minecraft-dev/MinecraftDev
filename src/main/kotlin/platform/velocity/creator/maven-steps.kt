/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addMavenGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractPatchPomStep
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.MavenImportStep
import com.demonwav.mcdev.creator.buildsystem.ReformatPomStep
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlTag
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel

class VelocityMavenSupport : BuildSystemSupport {
    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> VelocityMavenFilesStep(parent).nextStep(::VelocityPatchPomStep)
            BuildSystemSupport.POST_STEP -> MavenImportStep(parent)
                .nextStep(::ReformatPomStep)
                .nextStep { VelocityModifyMainClassStep(it, false) }
            else -> EmptyStep(parent)
        }
    }
}

class VelocityMavenFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Maven files"

    override fun setupAssets(project: Project) {
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        assets.addTemplateProperties(
            "JAVA_VERSION" to javaVersion,
        )
        assets.addTemplates(
            project,
            "pom.xml" to MinecraftTemplates.VELOCITY_POM_TEMPLATE,
        )
        if (gitEnabled) {
            assets.addMavenGitignore(project)
        }
    }
}

class VelocityPatchPomStep(parent: NewProjectWizardStep) : AbstractPatchPomStep(parent) {
    override fun patchPom(model: MavenDomProjectModel, root: XmlTag) {
        super.patchPom(model, root)

        val velocityApiVersion = data.getUserData(VelocityVersionStep.KEY) ?: return

        val annotationArtifactId =
            if (velocityApiVersion >= VelocityConstants.API_4) "velocity-annotation-processor" else "velocity-api"
        setupDependencies(
            model,
            listOf(
                BuildRepository(
                    "papermc-repo",
                    "https://repo.papermc.io/repository/maven-public/",
                ),
            ),
            listOf(
                BuildDependency(
                    "com.velocitypowered",
                    "velocity-api",
                    velocityApiVersion.toString(),
                    mavenScope = "provided",
                ),
                BuildDependency(
                    "com.velocitypowered",
                    annotationArtifactId,
                    velocityApiVersion.toString(),
                    mavenScope = if (velocityApiVersion >= VelocityConstants.API_4) "provided" else null,
                ),
            ),
        )
    }
}
