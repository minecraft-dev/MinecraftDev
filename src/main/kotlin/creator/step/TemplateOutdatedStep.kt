/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.step

import com.demonwav.mcdev.update.PluginUtil
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.ui.dsl.builder.Panel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TemplateOutdatedStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    override fun setupUI(builder: Panel) {
        with(builder) {
            separator()
            row {
                val issueUrl = "https://github.com/minecraft-dev/MinecraftDev/issues/new" +
                    "?template=project_wizard_outdated.yaml" +
                    "&plugin-version=${PluginUtil.pluginVersion.urlEncode()}" +
                    "&intellij-version=${ApplicationInfo.getInstance().build.asString().urlEncode()}" +
                    "&operating-system=${SystemInfoRt.OS_NAME.urlEncode()}"
                text(
                    "Is the Minecraft project wizard outdated? " +
                        "<a href=\"$issueUrl\">Create an issue</a> on the MinecraftDev issue tracker."
                )
            }
        }
    }

    private fun String.urlEncode() = URLEncoder.encode(this, StandardCharsets.UTF_8)
}
