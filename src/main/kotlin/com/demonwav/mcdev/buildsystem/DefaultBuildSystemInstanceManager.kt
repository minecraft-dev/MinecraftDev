/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem

import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem
import com.google.common.collect.Maps
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile

object DefaultBuildSystemInstanceManager : BuildSystemInstanceManager {
    private val map = Maps.newConcurrentMap<Module, BuildSystem?>()

    override fun getBuildSystem(module: Module): BuildSystem? {
        return map.computeIfAbsent(module) {
            val roots = ModuleRootManager.getInstance(module).contentRoots
            if (roots.isNotEmpty()) {
                var root: VirtualFile? = roots[0]

                if (root != null) {
                    var pom = root.findChild("pom.xml")
                    var gradle = root.findChild("build.gradle") ?:
                        root.findChild("settings.gradle") ?: root.findChild("build.gradle.kts")

                    if (pom != null) {
                        return@computeIfAbsent MavenBuildSystem()
                    } else if (gradle != null) {
                        return@computeIfAbsent GradleBuildSystem()
                    } else {
                        // We need to check if this is a multi-module gradle project
                        val project = module.project
                        val paths = ModuleManager.getInstance(project).getModuleGroupPath(module)

                        if (paths != null && paths.isNotEmpty()) {
                            // The first element is the parent
                            val parentName = paths[0]
                            val parentModule = ModuleManager.getInstance(project).findModuleByName(parentName)

                            if (parentModule != null) {
                                root = ModuleRootManager.getInstance(parentModule).contentRoots[0]
                                pom = root!!.findChild("pom.xml")

                                gradle = root.findChild("build.gradle") ?:
                                    root.findChild("settings.gradle") ?: root.findChild("build.gradle.kts")

                                if (pom != null) {
                                    return@computeIfAbsent MavenBuildSystem()
                                } else if (gradle != null) {
                                    return@computeIfAbsent GradleBuildSystem()
                                }
                            }
                        }
                    }
                }
            }
            null
        }
    }
}
