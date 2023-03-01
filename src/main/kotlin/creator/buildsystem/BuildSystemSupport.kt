/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
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
