/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.debug

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.DebuggingRunnerData
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element

class MixinRunConfigurationExtension : RunConfigurationExtension() {

    override fun isApplicableFor(configuration: RunConfigurationBase): Boolean {
        return configuration is ModuleBasedConfiguration<*>
    }

    override fun <T : RunConfigurationBase?> updateJavaParameters(configuration: T, params: JavaParameters?, runnerSettings: RunnerSettings?) {}

    override fun attachToProcess(configuration: RunConfigurationBase, handler: ProcessHandler, runnerSettings: RunnerSettings?) {
        // Check if we are in a debug run
        if (runnerSettings !is DebuggingRunnerData) {
            return
        }

        val config = configuration as ModuleBasedConfiguration<*>
        val module = config.configurationModule.module ?: return
        if (MinecraftFacet.getInstance(module)?.isOfType(MixinModuleType) == true) {
            // Add marker data to enable Mixin debugger
            handler.putUserData(MIXIN_DEBUG_KEY, true)
        }
    }

    override fun readExternal(runConfiguration: RunConfigurationBase, element: Element) {}

    override fun getEditorTitle(): String? = null
    override fun <P : RunConfigurationBase?> createEditor(configuration: P): SettingsEditor<P>? = null
}
