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
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.util.Key
import com.demonwav.mcdev.util.runWriteTask
import com.google.common.base.MoreObjects
import com.google.common.base.Objects
import com.google.common.collect.Maps
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.Contract
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.Optional

/**
 * Base class for Maven and Gradle build systems. The general contract of any class which implements this is any
 * change in a setter in this class will reflect back on the actual files that these classes represent, and in turn
 * represent changes in the project itself.
 */
abstract class BuildSystem {

    private val extraData = Maps.newHashMap<Key<*>, Any>()

    lateinit var artifactId: String
    lateinit var groupId: String
    lateinit var version: String

    var dependencies: List<BuildDependency> = ArrayList()
    var repositories: List<BuildRepository> = ArrayList()
    lateinit var rootDirectory: VirtualFile

    var sourceDirectories: List<VirtualFile?> = ArrayList()
    var resourceDirectories: List<VirtualFile?> = ArrayList()
    var testSourcesDirectories: List<VirtualFile?> = ArrayList()
    var testResourceDirectories: List<VirtualFile?> = ArrayList()

    var importPromise: AsyncPromise<BuildSystem>? = null

    /**
     * This refers to the plugin name from the perspective of the build system, that being a name field in the build
     * system's configuration. This is not the actual plugin name, which would be stated in the plugin's description
     * file, or the main class file, depending on the project. This field is null if this value is missing.
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

    /**
     * This method performs similarly to [.create] in that it builds
     * this object's model of the project. The difference here is this method reads the project and builds the model
     * from the current project's state. The includes settings the artifactId, groupId, and version, setting the root
     * directory, building the list of dependencies and repositories, settings the source, test, and resource directories,
     * and setting the build version, and whatever else may be added that consists of this project's build system state.

     * @param module The module
     */
    abstract fun reImport(module: Module): Promise<out BuildSystem>

    protected fun synchronize(): Boolean {
        synchronized(lock) {
            if (importPromise != null && importPromise!!.state != Promise.State.FULFILLED) {
                // we're in the process of importing
                return true
            }

            importPromise = AsyncPromise<BuildSystem>()
            return false
        }
    }

    fun findFile(path: String, type: SourceType): VirtualFile? {
        when (type) {
            SourceType.SOURCE -> return findFile(sourceDirectories, path)
            SourceType.RESOURCE -> return findFile(resourceDirectories, path)
            SourceType.TEST_SOURCE -> return findFile(testSourcesDirectories, path)
            SourceType.TEST_RESOURCE -> return findFile(testResourceDirectories, path)
            else -> return null
        }
    }

    @Contract("null, _ -> null")
    private fun findFile(dirs: List<VirtualFile?>?, path: String): VirtualFile? {
        var file: VirtualFile?
        if (dirs == null) {
            return null
        }

        for (dir in dirs) {
            file = dir?.findFileByRelativePath(path)
            if (file != null) {
                return file
            }
        }
        return null
    }

    @JvmOverloads
    protected fun createDirectories(root: VirtualFile = rootDirectory) {
        runWriteTask {
            try {
                sourceDirectories = arrayListOf(VfsUtil.createDirectories(root.path + "/src/main/java"))
                resourceDirectories = arrayListOf(VfsUtil.createDirectories(root.path + "/src/main/resources"))
                testSourcesDirectories = arrayListOf(VfsUtil.createDirectories(root.path + "/src/test/java"))
                testResourceDirectories = arrayListOf(VfsUtil.createDirectories(root.path + "/src/test/resources"))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun <T> setData(key: Key<T>, data: T) {
        extraData.put(key, data)
    }

    @Contract(pure = true)
    fun <T> getData(key: Key<T>?): Optional<T> {
        @Suppress("UNCHECKED_CAST")
        return Optional.ofNullable(extraData[key] as T)
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
            .add("extraData", extraData)
            .add("artifactId", artifactId)
            .add("groupId", groupId)
            .add("version", version)
            .add("dependencies", dependencies)
            .add("repositories", repositories)
            .add("rootDirectory", rootDirectory)
            .add("sourceDirectories", sourceDirectories)
            .add("resourceDirectories", resourceDirectories)
            .add("testSourcesDirectories", testSourcesDirectories)
            .add("testResourceDirectories", testResourceDirectories)
            .add("importPromise", importPromise)
            .add("pluginName", pluginName)
            .add("buildVersion", buildVersion)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as BuildSystem
        return Objects.equal(artifactId, that.artifactId) &&
            Objects.equal(groupId, that.groupId) &&
            Objects.equal(version, that.version) &&
            Objects.equal(dependencies, that.dependencies) &&
            Objects.equal(repositories, that.repositories) &&
            Objects.equal(rootDirectory, that.rootDirectory) &&
            Objects.equal(sourceDirectories, that.sourceDirectories) &&
            Objects.equal(resourceDirectories, that.resourceDirectories) &&
            Objects.equal(testSourcesDirectories, that.testSourcesDirectories) &&
            Objects.equal(testResourceDirectories, that.testResourceDirectories) &&
            Objects.equal(pluginName, that.pluginName) &&
            Objects.equal(buildVersion, that.buildVersion)
    }

    override fun hashCode(): Int {
        return Objects.hashCode(artifactId, groupId, version, dependencies, repositories, rootDirectory, sourceDirectories,
            resourceDirectories, testSourcesDirectories, testResourceDirectories, pluginName, buildVersion
        )
    }

    companion object {
        private val lock = Any()

        private val map = HashMap<Module, BuildSystem?>()

        @JvmStatic
        fun getInstance(module: Module): BuildSystem? {
            // generally the other way around, by synchronizing this block and the reImport for GradleBuildSystem and
            // MavenBuildSystem, performance is significantly increased, as it dramatically decreases the number of times
            // the module will be reimported.
            synchronized(lock) {
                return map.computeIfAbsent(module) {
                    val roots = ModuleRootManager.getInstance(module).contentRoots
                    if (roots.isNotEmpty()) {
                        var root: VirtualFile? = roots[0]
                        if (root != null) {
                            var pom = root.findChild("pom.xml")
                            var gradle = root.findChild("build.gradle")

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
                                        gradle = root.findChild("build.gradle")

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
    }
}
