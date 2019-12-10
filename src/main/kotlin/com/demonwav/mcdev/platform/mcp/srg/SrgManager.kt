/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
                rejectedPromise("No mapping data available")
            } else {
                runAsync {
                    // Load SRG map from files
                    srgType.srgParser.parseSrg(Paths.get(file))
                }
            }
        } else {
            // Path to SRG files is unknown
            rejectedPromise("No mapping data available")
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
