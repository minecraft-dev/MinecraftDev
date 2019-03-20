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
abstract class BuildSystem(
    val artifactId: String,
    val groupId: String,
    val version: String
) {
    var directories: DirectorySet? = null

    val dependencies = mutableListOf<BuildDependency>()
    val repositories = mutableListOf<BuildRepository>()

    abstract fun create(
        project: Project,
        rootDirectory: VirtualFile,
        configuration: ProjectConfiguration,
        indicator: ProgressIndicator,
        pluginName: String
    )

    abstract fun finishSetup(
        rootModule: Module,
        rootDirectory: VirtualFile,
        configurations: Collection<ProjectConfiguration>,
        indicator: ProgressIndicator
    )

    data class DirectorySet(
        val sourceDirectory: VirtualFile,
        val resourceDirectory: VirtualFile,
        val testSourceDirectory: VirtualFile,
        val testResourceDirectory: VirtualFile
    )

    fun createDirectories(dir: VirtualFile): DirectorySet {
        val sourceDirectory = VfsUtil.createDirectories(dir.path + "/src/main/java")
        val resourceDirectory = VfsUtil.createDirectories(dir.path + "/src/main/resources")
        val testSourceDirectory = VfsUtil.createDirectories(dir.path + "/src/test/java")
        val testResourceDirectory = VfsUtil.createDirectories(dir.path + "/src/test/resources")
        return DirectorySet(sourceDirectory, resourceDirectory, testSourceDirectory, testResourceDirectory)
    }
}
