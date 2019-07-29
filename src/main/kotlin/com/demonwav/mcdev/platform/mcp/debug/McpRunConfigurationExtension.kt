/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.debug

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.ModuleDebugRunConfigurationExtension
import com.intellij.debugger.DebuggerManager
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.DebugProcessListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.module.Module

class McpRunConfigurationExtension : ModuleDebugRunConfigurationExtension(), DebugProcessListener {

    override fun attachToProcess(handler: ProcessHandler, module: Module) {
        if (MinecraftFacet.getInstance(module)?.isOfType(McpModuleType) == true) {
            DebuggerManager.getInstance(module.project).addDebugProcessListener(handler, this)
        }
    }

    override fun processAttached(process: DebugProcess) {
        if (process !is DebugProcessImpl) {
            return
        }

        // Add session listener
        process.xdebugProcess?.session?.addSessionListener(UngrabMouseDebugSessionListener(process))

        // We don't need any further events
        process.removeDebugProcessListener(this)
    }
}
