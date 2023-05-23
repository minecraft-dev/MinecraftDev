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

package com.demonwav.mcdev.creator.buildsystem

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.openapi.util.KeyedExtensionCollector
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute

interface BuildSystemSupport {
    companion object {
        private val EP_NAME = ExtensionPointName<KeyedLazyInstance<BuildSystemSupport>>(
            "com.demonwav.minecraft-dev.buildSystemSupport",
        )
        private val COLLECTOR = KeyedExtensionCollector<BuildSystemSupport, Pair<String, String>>(EP_NAME)

        fun getInstance(platform: String, buildSystem: String): BuildSystemSupport? =
            COLLECTOR.findSingle(platform to buildSystem)

        const val PRE_STEP = "pre"
        const val POST_STEP = "post"
    }

    fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep

    val preferred get() = false
}

class BuildSystemSupportEntry : BaseKeyedLazyInstance<BuildSystemSupport>(), KeyedLazyInstance<BuildSystemSupport> {
    @Attribute("implementation")
    @RequiredElement
    lateinit var implementation: String

    @Attribute("platform")
    @RequiredElement
    lateinit var platform: String

    @Attribute("buildSystem")
    @RequiredElement
    lateinit var buildSystem: String

    override fun getKey() = (platform to buildSystem).toString()

    override fun getImplementationClassName() = implementation
}
