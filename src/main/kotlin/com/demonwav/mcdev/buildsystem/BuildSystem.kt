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

import com.demonwav.mcdev.platform.ProjectConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

/**
 * Base class for Maven and Gradle build systems. The general contract of any class which implements this is any
 * change in a setter in this class will reflect back on the actual files that these classes represent, and in turn
 * represent changes in the project itself.
 */
abstract class BuildSystem {

    private val lock = Any()

    val dependencies = mutableSetOf<BuildDependency>()
    val repositories = mutableSetOf<BuildRepository>()

    lateinit var artifactId: String
    lateinit var groupId: String
    lateinit var version: String

    lateinit var rootDirectory: VirtualFile

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
     * *
     * @param configurations The configuration objects for the project
     */
    abstract fun create(project: Project,
                        configurations: ProjectConfiguration,
                        indicator: ProgressIndicator)

    /**
     * This is called after [.create], and after the module has set
     * itself up. This is when the build system should make whatever calls are necessary to enable the build system's
     * plugin, and setup whatever run configs should be setup for this build system.
     *
     *
     * It is legal for this method to have different default setups for each platform type, so the PlatformType and
     * ProjectConfiguration are provided here as well.

     * @param module the module
     * *
     * @param configurations The configuration object for the project
     */
    abstract fun finishSetup(module: Module,
                             configurations: Collection<ProjectConfiguration>,
                             indicator: ProgressIndicator)

    @JvmOverloads
    fun createDirectories(dir: VirtualFile = rootDirectory) {
        VfsUtil.createDirectories(dir.path + "/src/main/java")
        VfsUtil.createDirectories(dir.path + "/src/main/resources")
        VfsUtil.createDirectories(dir.path + "/src/test/java")
        VfsUtil.createDirectories(dir.path + "/src/test/resources")
    }

    companion object {
        var instanceManager: BuildSystemInstanceManager = DefaultBuildSystemInstanceManager

        @JvmStatic
        fun getInstance(module: Module) = instanceManager.getBuildSystem(module)
    }
}
