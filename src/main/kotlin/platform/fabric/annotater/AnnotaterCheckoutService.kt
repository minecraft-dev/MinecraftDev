/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.fabric.annotater

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.minecraft.MinecraftModuleSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * Finds the annotater checkout to use for Minecraft classes.
 */
@Service(Service.Level.APP)
class AnnotaterCheckoutService {
    private val checkouts = ConcurrentHashMap<Path, AnnotaterCheckout>()

    init {
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            AnnotaterListener.TOPIC,
            object : AnnotaterListener {
                override fun checkoutsChanged() {
                    checkouts.clear()
                }
            },
        )
    }

    sealed class Status {
        data class Available(val checkout: AnnotaterCheckout) : Status()
        data object Disabled : Status()
        data object UnknownMinecraftVersion : Status()
        data class NotCloned(val version: String) : Status()
        data class VersionMismatch(val expected: String, val actual: String) : Status()
        data class Invalid(val root: Path, val message: String) : Status()
    }

    /**
     * Finds the checkout for [version] according to the user's settings, ignoring whether the feature is enabled.
     */
    fun statusForVersion(version: String): Status {
        val settings = AnnotaterSettings.instance
        return when (settings.checkoutMode) {
            AnnotaterSettings.CheckoutMode.MANAGED -> {
                val root = managedWorktree(version)
                if (!root.resolve(AnnotaterConfig.FILE_NAME).exists()) {
                    return Status.NotCloned(version)
                }
                load(root, managed = true)
            }

            AnnotaterSettings.CheckoutMode.CUSTOM -> {
                if (settings.customDirectory.isBlank()) {
                    return Status.Invalid(Path(""), "No annotater directory selected")
                }
                when (val status = load(Path(settings.customDirectory), managed = false)) {
                    is Status.Available -> if (status.checkout.version != version) {
                        Status.VersionMismatch(version, status.checkout.version)
                    } else {
                        status
                    }

                    else -> status
                }
            }
        }
    }

    fun checkoutForVersion(version: String): AnnotaterCheckout? {
        return (statusForVersion(version) as? Status.Available)?.checkout
    }

    fun status(element: PsiElement): Status {
        if (!AnnotaterSettings.instance.enabled) {
            return Status.Disabled
        }
        val file = PsiUtilCore.getVirtualFile(element) ?: return Status.UnknownMinecraftVersion
        val version = minecraftVersion(element.project, file) ?: return Status.UnknownMinecraftVersion
        return statusForVersion(version)
    }

    /**
     * The checkout to use for [element], or null if annotater is disabled, [element] isn't part of Minecraft, or no
     * checkout exists for its Minecraft version.
     */
    fun checkoutFor(element: PsiElement): AnnotaterCheckout? {
        if (!AnnotaterSettings.instance.enabled) {
            return null
        }
        val file = PsiUtilCore.getVirtualFile(element) ?: return null
        return checkoutFor(element.project, file)
    }

    /**
     * The checkout to use for [file] in [project], or null if annotater is disabled, [file] isn't part of Minecraft,
     * or no checkout exists for its Minecraft version.
     */
    fun checkoutFor(project: Project, file: VirtualFile): AnnotaterCheckout? {
        if (!AnnotaterSettings.instance.enabled || !isMinecraftFile(project, file)) {
            return null
        }
        val version = minecraftVersion(project, file) ?: return null
        return checkoutForVersion(version)
    }

    /**
     * Updates cached data after files changed on disk, returning the root and outer class of each changed Javadoc
     * file, and whether a `config.json` changed.
     */
    fun filesChanged(paths: Collection<Path>): Pair<List<Pair<Path, String>>, Boolean> {
        val changedJavadocs = mutableListOf<Pair<Path, String>>()
        var configChanged = false
        for (checkout in checkouts.values) {
            for (path in paths) {
                if (path == checkout.root.resolve(AnnotaterConfig.FILE_NAME)) {
                    configChanged = true
                } else if (path.startsWith(checkout.mappingsDir) && path.fileName.toString().endsWith(".mapping")) {
                    val outerClass = checkout.mappingsDir.relativize(path).joinToString("/").removeSuffix(".mapping")
                    checkout.javadocStore.invalidate(outerClass)
                    changedJavadocs += checkout.root to outerClass
                }
            }
        }
        return changedJavadocs to configChanged
    }

    fun managedWorktree(version: String): Path = AnnotaterSettings.instance.effectiveManagedRoot.resolve(version)

    private fun load(root: Path, managed: Boolean): Status {
        checkouts[root]?.let { return Status.Available(it) }
        return try {
            val checkout = AnnotaterCheckout(root, AnnotaterConfig.read(root), managed)
            Status.Available(checkouts.putIfAbsent(root, checkout) ?: checkout)
        } catch (e: IOException) {
            thisLogger().debug("Failed to read annotater checkout at $root", e)
            Status.Invalid(root, e.message ?: e.toString())
        }
    }

    companion object {
        private val IS_MINECRAFT_ROOT = Key.create<Boolean>("mcdev.annotater.isMinecraftRoot")

        val instance: AnnotaterCheckoutService
            get() = ApplicationManager.getApplication().getService(AnnotaterCheckoutService::class.java)

        /**
         * Whether any module of [project] is a Fabric mod. Annotater is a Fabric project, so it's offered more
         * prominently there.
         */
        fun isFabricProject(project: Project): Boolean {
            return ModuleManager.getInstance(project).modules.any { MinecraftFacet.getInstance(it, FabricModuleType) != null }
        }

        /**
         * Whether [file] is a class or source file from a Minecraft jar, as opposed to project sources or another
         * library.
         */
        fun isMinecraftFile(project: Project, file: VirtualFile): Boolean {
            val fileIndex = ProjectFileIndex.getInstance(project)
            if (!fileIndex.isInLibrary(file)) {
                return false
            }
            val root = fileIndex.getClassRootForFile(file) ?: fileIndex.getSourceRootForFile(file) ?: return false
            root.getUserData(IS_MINECRAFT_ROOT)?.let { return it }
            val isMinecraft = root.findFileByRelativePath("net/minecraft") != null
            root.putUserData(IS_MINECRAFT_ROOT, isMinecraft)
            return isMinecraft
        }

        /**
         * The Minecraft version of the modules that depend on the library containing [file].
         */
        fun minecraftVersion(project: Project, file: VirtualFile): String? {
            val fileIndex = ProjectFileIndex.getInstance(project)
            val modules = fileIndex.getOrderEntriesForFile(file).asSequence().map { it.ownerModule } +
                ModuleManager.getInstance(project).modules.asSequence()
            return modules
                .mapNotNull { module ->
                    if (module.isDisposed) null else MinecraftModuleSettings.getInstance(module).state.minecraftVersion
                }
                .firstOrNull { it.isNotBlank() }
        }
    }
}
