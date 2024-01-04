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

class McpRunConfigurationExtension : ModuleDebugRunConfigurationExtension() {

    override fun attachToProcess(handler: ProcessHandler, module: Module) {
        if (MinecraftFacet.getInstance(module)?.isOfType(McpModuleType) == true) {
            DebuggerManager.getInstance(module.project)
                .addDebugProcessListener(handler, MyProcessListener())
        }
    }

    private inner class MyProcessListener : DebugProcessListener {

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
}
