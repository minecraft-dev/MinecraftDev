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

package com.demonwav.mcdev.platform.mcp.srg

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
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
import org.jetbrains.concurrency.runAsync

class SrgManager(val file: String, val srgType: SrgType) {

    var srgMap: Promise<McpSrgMap> = rejectedPromise("SRG map not loaded")
        @Synchronized get
        private set

    val srgMapNow: McpSrgMap?
        @Synchronized get() = try {
            srgMap.blockingGet(1, TimeUnit.NANOSECONDS)
        } catch (e: ExecutionException) {
            null
        } catch (e: TimeoutException) {
            null
        }

    @Synchronized
    fun parse() {
        if (srgMap.state == Promise.State.PENDING) {
            return
        }

        srgMap = if (file.isNotBlank()) {
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

    companion object {
        private val map = HashMap<String, SrgManager>()

        fun getInstance(file: String, srgType: SrgType) = map.computeIfAbsent(file) { SrgManager(it, srgType) }

        fun findAnyInstance(project: Project) =
            ModuleManager.getInstance(project).modules.mapFirstNotNull {
                MinecraftFacet.getInstance(it, McpModuleType)?.srgManager
            }
    }
}
