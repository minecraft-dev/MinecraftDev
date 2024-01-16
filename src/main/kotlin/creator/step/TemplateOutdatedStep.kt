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

package com.demonwav.mcdev.creator.step

import com.demonwav.mcdev.asset.MCDevBundle
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
                text(MCDevBundle("creator.ui.outdated.message", issueUrl))
            }
        }
    }

    private fun String.urlEncode() = URLEncoder.encode(this, StandardCharsets.UTF_8)
}
