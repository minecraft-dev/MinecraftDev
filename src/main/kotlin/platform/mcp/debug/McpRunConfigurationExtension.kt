/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
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
import com.intellij.openapi.module.ModulePointer
import com.intellij.openapi.module.ModulePointerManager

class McpRunConfigurationExtension : ModuleDebugRunConfigurationExtension() {

    override fun attachToProcess(handler: ProcessHandler, module: Module) {
        if (MinecraftFacet.getInstance(module)?.isOfType(McpModuleType) == true) {
            val modulePointer = ModulePointerManager.getInstance(module.project).create(module)
            DebuggerManager.getInstance(module.project)
                .addDebugProcessListener(handler, MyProcessListener(modulePointer))
        }
    }

    private inner class MyProcessListener(private val modulePointer: ModulePointer) : DebugProcessListener {

        override fun processAttached(process: DebugProcess) {
            if (process !is DebugProcessImpl) {
                return
            }

            // Add session listener
            process.xdebugProcess?.session?.addSessionListener(UngrabMouseDebugSessionListener(process, modulePointer))

            // We don't need any further events
            process.removeDebugProcessListener(this)
        }
    }
}
