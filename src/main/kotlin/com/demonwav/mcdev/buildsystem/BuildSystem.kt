/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem

import com.demonwav.mcdev.platform.ProjectConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

/**
 * Base class for build system project creation.
 */
abstract class BuildSystem {

    val dependencies = mutableListOf<BuildDependency>()
    val repositories = mutableListOf<BuildRepository>()

    lateinit var artifactId: String
    lateinit var groupId: String
    lateinit var version: String

    lateinit var rootDirectory: VirtualFile

    lateinit var sourceDirectory: VirtualFile
    lateinit var resourceDirectory: VirtualFile
    lateinit var testSourceDirectory: VirtualFile
    lateinit var testResourceDirectory: VirtualFile

    /**
     * This refers to the plugin name from the perspective of the build system, that being a name field in the build
     * system's configuration. This is not the actual plugin name, which would be stated in the plugin's description
     * file, or the main class file, depending on the project.
     */
    lateinit var pluginName: String

    lateinit var buildVersion: String

    /**
     * Assuming the artifact ID, group ID, and  version are set, along with whatever dependencies and repositories and
     * the root directory, create a base module consisting of the necessary build system configuration files and
     * directory structure. This method does not create any classes or project-specific things, nor does it set up
     * any build configurations or enable the plugin for this build config. This will be done in
     * [finishSetup].
     *
     *
     * It is legal for this method to have different default setups for each platform type, so the PlatformType and
     * ProjectConfiguration are provided here as well.

     * @param project The project
     * @param configuration The configuration object for the project
     * @param indicator The progress indicator
     */
    abstract fun create(project: Project, configuration: ProjectConfiguration, indicator: ProgressIndicator)

    /**
     * This is called after [create], and after the module has set
     * itself up. This is when the build system should make whatever calls are necessary to enable the build system's
     * plugin, and setup whatever run configs should be setup for this build system.
     *
     *
     * It is legal for this method to have different default setups for each platform type, so the PlatformType and
     * ProjectConfiguration are provided here as well.

     * @param rootModule the root module
     * @param configurations The configuration object for the project
     * @param indicator The progress indicator
     */
    abstract fun finishSetup(rootModule: Module, configurations: Collection<ProjectConfiguration>, indicator: ProgressIndicator)

    fun createDirectories(dir: VirtualFile = rootDirectory) {
        sourceDirectory = VfsUtil.createDirectories(dir.path + "/src/main/java")
        resourceDirectory = VfsUtil.createDirectories(dir.path + "/src/main/resources")
        testSourceDirectory = VfsUtil.createDirectories(dir.path + "/src/test/java")
        testResourceDirectory = VfsUtil.createDirectories(dir.path + "/src/test/resources")
    }
}
