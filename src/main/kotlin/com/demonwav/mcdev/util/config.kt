/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.io.FileUtil
import com.intellij.project.stateStore
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.io.inputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.TreeMap
import kotlin.streams.asSequence

abstract class VersionedConfig<V>(private val name: String, private val valueType: Type) {
    private val gson = GsonBuilder().apply {
        registerTypeAdapter(ConfigFile::class.java, ConfigFileDeserializer())
        setup()
    }.create()
    private val builtinEntries by lazy { build(load(javaClass.getResource("/configs/$name").toURI())) }
    private val globalModificationTracker = ConfigModificationTracker()

    operator fun get(element: PsiElement): List<V> {
        val version = element.mcVersion ?: return listOf()
        return getProjectEntries(element.project).floorEntry(version)?.value ?: emptyList()
    }

    private fun build(files: Map<SemanticVersion, ConfigFile>): TreeMap<SemanticVersion, List<V>> {
        val sorted = files.entries.sortedBy { it.key }
        return TreeMap(
            files.mapValues { ref ->
                // For each version, we want to inherit the values from the older version, unless the config specifies otherwise
                // Hence, for each entry, collect all older versions and merge their lists of config values
                sorted.filter { it.key <= ref.key }
                    .takeLastUntil { !it.value.inherit }
                    .asSequence()
                    .map { it.value.entries }
                    .reduce { acc, cur ->
                        acc.filter { older -> cur.none { newer -> newer.overrides(older) } } + cur
                    }
            }
        )
    }

    private fun <T> List<T>.takeLastUntil(predicate: (T) -> Boolean): List<T> {
        val iterator = listIterator(size)
        while (iterator.hasPrevious()) {
            if (predicate(iterator.previous())) {
                val expectedSize = size - iterator.nextIndex()
                if (expectedSize == 0) return emptyList()
                return ArrayList<T>(expectedSize).apply {
                    while (iterator.hasNext())
                        add(iterator.next())
                }
            }
        }
        return this
    }

    private fun load(uri: URI): Map<SemanticVersion, ConfigFile> {
        return uri.listFiles()
            .filter { it.fileName.toString().endsWith(".json") }
            .associate {
                val version = SemanticVersion.parse(it.fileName.toString().substringBeforeLast('.'))
                version to load(it.inputStream())
            }
    }

    private fun URI.listFiles(): Sequence<Path> {
        val parts = this.toString().split("!", limit = 2)
        val path = when (parts.size) {
            1 -> Paths.get(this)
            else -> getOrCreateFileSystem(URI.create(parts[0])).getPath(parts[1])
        }
        return if (Files.exists(path)) Files.list(path).asSequence() else emptySequence()
    }

    private fun getOrCreateFileSystem(uri: URI): FileSystem =
        try {
            FileSystems.getFileSystem(uri)
        } catch (ex: FileSystemNotFoundException) {
            FileSystems.newFileSystem(uri, emptyMap<String, String>())
        }

    private fun load(stream: InputStream): ConfigFile {
        val content = InputStreamReader(stream)
        return gson.fromJson(content, typeToken<ConfigFile>())
    }

    private fun getProjectEntries(project: Project) =
        CachedValuesManager.getManager(project).getCachedValue(
            project,
            Key("project.mcdev_configs.$name"),
            {
                val globalEntries = getGlobalEntries(project)
                val projectEntries = build(getProjectConfigFiles(project))
                val merged = merge(merge(builtinEntries, globalEntries), projectEntries)
                CachedValueProvider.Result.create(merged, getProjectModificationTracker(project))
            },
            false
        )

    private fun getGlobalEntries(project: Project) =
        CachedValuesManager.getManager(project).getCachedValue(
            project,
            Key("mcdev_configs.$name"),
            { CachedValueProvider.Result.create(build(getGlobalConfigFiles()), globalModificationTracker) },
            false
        )

    private fun merge(lowPriority: TreeMap<SemanticVersion, List<V>>, highPriority: TreeMap<SemanticVersion, List<V>>): TreeMap<SemanticVersion, List<V>> {
        val result = TreeMap(lowPriority)
        for ((version, entries) in highPriority) {
            if (version !in result) {
                result[version] = entries
            } else {
                val current = result[version] ?: listOf()
                result[version] = current.filter { lower -> entries.none { higher -> higher.overrides(lower) } } + entries
            }
        }
        return result
    }

    fun getGlobalConfigFiles(): Map<SemanticVersion, ConfigFile> {
        val path = Paths.get(PathManager.getConfigPath(), "mcdev_configs", name)
        return load(path.toUri())
    }

    fun getProjectConfigFiles(project: Project): Map<SemanticVersion, ConfigFile> {
        val path = Paths.get(
            FileUtil.toSystemDependentName(project.stateStore.getDirectoryStorePath(false)!!),
            "mcdev_configs",
            name
        )
        return load(path.toUri())
    }

    protected abstract fun GsonBuilder.setup()

    protected abstract fun V.overrides(older: V): Boolean

    protected abstract fun getProjectModificationTracker(project: Project): ConfigModificationTracker

    inner class ConfigFile(val inherit: Boolean, val entries: List<V>)

    private inner class ConfigFileDeserializer : JsonDeserializer<ConfigFile> {
        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): ConfigFile {
            val obj = json.asJsonObject
            val inherit = obj.get("inherit")?.asBoolean ?: true
            return ConfigFile(inherit, obj.getAsJsonArray("entries").map { ctx.deserialize<V>(it, valueType) })
        }
    }

    class ConfigModificationTracker : ModificationTracker {
        private var modificationCount = 0L

        fun update() {
            modificationCount += 1L
        }

        override fun getModificationCount() = modificationCount
    }
}

inline fun <reified T> typeToken(): Type = object : TypeToken<T>() {}.type
