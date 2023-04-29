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

import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.splitPackage
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractLongRunningStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DependStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.MainClassStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTaskInSmartMode
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.nio.file.Path

class VelocityProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating project files"

    override fun setupAssets(project: Project) {
        val mainClass = data.getUserData(MainClassStep.KEY) ?: return
        val dependencies = data.getUserData(DependStep.KEY) ?: emptyList()
        val (packageName, className) = splitPackage(mainClass)
        val version = data.getUserData(VelocityVersionStep.KEY) ?: return

        assets.addTemplateProperties(
            "PACKAGE" to packageName,
            "CLASS_NAME" to className,
        )

        if (dependencies.isNotEmpty()) {
            assets.addTemplateProperties(
                "HAS_DEPENDENCIES" to "true",
            )
        }

        val template = if (version < VelocityConstants.API_2 ||
            (version >= VelocityConstants.API_3 && version < VelocityConstants.API_4)
        ) {
            MinecraftTemplates.VELOCITY_MAIN_CLASS_TEMPLATE // API 1 and 3
        } else {
            MinecraftTemplates.VELOCITY_MAIN_CLASS_V2_TEMPLATE // API 2 and 4 (4+ maybe ?)
        }

        assets.addTemplates(
            project,
            "src/main/java/${mainClass.replace('.', '/')}.java" to template,
        )
    }
}

class VelocityModifyMainClassStep(
    parent: NewProjectWizardStep,
    private val isGradle: Boolean,
) : AbstractLongRunningStep(parent) {
    override val description = "Patching main class"

    override fun perform(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val pluginName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val mainClassName = data.getUserData(MainClassStep.KEY) ?: return
        val mainClassFile = "${context.projectFileDirectory}/src/main/java/${mainClassName.replace('.', '/')}.java"
        val description = data.getUserData(DescriptionStep.KEY) ?: ""
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val dependencies = data.getUserData(DependStep.KEY) ?: emptyList()

        NonProjectFileWritingAccessProvider.disableChecksDuring {
            project.runWriteTaskInSmartMode {
                val mainClassVirtualFile = VfsUtil.findFile(Path.of(mainClassFile), true)
                    ?: return@runWriteTaskInSmartMode
                val mainClassPsi = PsiManager.getInstance(project).findFile(mainClassVirtualFile) as? PsiJavaFile
                    ?: return@runWriteTaskInSmartMode

                val psiClass = mainClassPsi.classes[0]
                val annotation = buildString {
                    append("@Plugin(")
                    append("\nid = ${literal(buildSystemProps.artifactId)}")
                    append(",\nname = ${literal(pluginName)}")

                    if (isGradle) {
                        append(",\nversion = BuildConstants.VERSION")
                    } else {
                        append(",\nversion = \"${buildSystemProps.version}\"")
                    }

                    if (description.isNotBlank()) {
                        append(",\ndescription = ${literal(description)}")
                    }

                    if (website.isNotBlank()) {
                        append(",\nurl = ${literal(website)}")
                    }

                    if (authors.isNotEmpty()) {
                        append(",\nauthors = {${authors.joinToString(", ", transform = ::literal)}}")
                    }

                    if (dependencies.isNotEmpty()) {
                        val deps = dependencies.joinToString(",\n") { "@Dependency(id = ${literal(it)})" }
                        append(",\ndependencies = {\n$deps\n}")
                    }

                    append("\n)")
                }

                val factory = JavaPsiFacade.getElementFactory(project)
                val pluginAnnotation = factory.createAnnotationFromText(annotation, null)

                mainClassPsi.runWriteAction {
                    psiClass.modifierList?.let { it.addBefore(pluginAnnotation, it.firstChild) }
                    CodeStyleManager.getInstance(project).reformat(psiClass)
                }
            }
        }
    }

    private fun literal(text: String?): String {
        if (text == null) {
            return "\"\""
        }
        return '"' + StringUtil.escapeStringCharacters(text) + '"'
    }
}

class VelocityBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Velocity"
}

class VelocityPostBuildSystemStep(parent: NewProjectWizardStep) : AbstractRunBuildSystemStep(
    parent,
    VelocityBuildSystemStep::class.java,
) {
    override val step = BuildSystemSupport.POST_STEP
}
