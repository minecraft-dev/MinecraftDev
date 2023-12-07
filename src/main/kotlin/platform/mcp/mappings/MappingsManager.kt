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

package com.demonwav.mcdev.platform.mcp.mappings

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.srg.SrgType
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.io.path.absolutePathString
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync

abstract class MappingsManager {
    var mappings: Promise<Mappings> = rejectedPromise("Mappings not loaded")
        @Synchronized get
        protected set

    val mappingsNow: Mappings?
        @Synchronized get() = try {
            mappings.blockingGet(1, TimeUnit.NANOSECONDS)
        } catch (e: ExecutionException) {
            null
        } catch (e: TimeoutException) {
            null
        }

    abstract fun parse()

    companion object {
        private val map = HashMap<String, MappingsManager>()

        @Deprecated("This needs replacing with something that doesn't cause memory leaks")
        fun getInstance(file: String, srgType: SrgType) = map.computeIfAbsent(file) { SrgMappingsManager(it, srgType) }

        fun findAnyInstance(project: Project) =
            ModuleManager.getInstance(project).modules.mapFirstNotNull {
                MinecraftFacet.getInstance(it, McpModuleType)?.mappingsManager
            }
    }

    class Immediate(mappings: Mappings) : MappingsManager() {
        init {
            this.mappings = resolvedPromise(mappings)
        }

        override fun parse() {
        }
    }

    private class SrgMappingsManager(val file: String, val srgType: SrgType) : MappingsManager() {
        @Synchronized
        override fun parse() {
            if (mappings.state == Promise.State.PENDING) {
                return
            }

            mappings = if (file.isNotBlank()) {
                val path = Paths.get(file)
                if (Files.notExists(path)) {
                    rejectedPromise("No mapping data available at " + path.absolutePathString())
                } else {
                    runAsync {
                        // Load SRG map from files
                        srgType.srgParser.parseSrg(Paths.get(file))
                    }
                }
            } else {
                // Path to SRG files is unknown
                rejectedPromise("No mapping data available: file path is blank")
            }
        }
    }
}
