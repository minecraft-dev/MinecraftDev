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
import com.google.gson.reflect.TypeToken
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
import kotlin.streams.toList

abstract class VersionedConfig<V>(private val path: String, private val valueType: Type) {
    private val gson = GsonBuilder().apply { setup() }.create()
    private val entries by lazy {
        val result = load()
        result.mapValues { ref ->
            // For each version, we want to inherit the values from the older version
            // Hence, for each entry, collect all older versions and merge their lists of config values
            result.entries.asSequence()
                .filter { it.key <= ref.key }
                .sortedBy { it.key }
                .map { it.value }
                .reduce { acc, cur ->
                    acc.filter { older -> cur.none { b -> b.overrides(older) } } + cur
                }
        }.toMutableMap()
    }

    val configuredVersions by lazy { entries.keys.toList() }

    operator fun get(mcVersion: String): List<V> {
        val semVer = SemanticVersion.parse(mcVersion)
        return entries.entries.findLast { it.key <= semVer }?.value ?: emptyList()
    }

    private fun load(): Map<SemanticVersion, List<V>> {
        val url = javaClass.getResource(path)
        val files = url.toURI().listFiles()
        return files
            .asSequence()
            .filter { it.fileName.toString().endsWith(".json") }
            .associate {
                val version = SemanticVersion.parse(it.fileName.toString().substringBeforeLast('.'))
                version to load(it.inputStream())
            }
    }

    private fun URI.listFiles(): List<Path> {
        val parts = this.toString().split("!", limit = 2)
        val path = when (parts.size) {
            1 -> Paths.get(this)
            else -> getOrCreateFileSystem(URI.create(parts[0])).getPath(parts[1])
        }
        return Files.list(path).toList()
    }

    private fun getOrCreateFileSystem(uri: URI): FileSystem =
        try {
            FileSystems.getFileSystem(uri)
        } catch (ex: FileSystemNotFoundException) {
            FileSystems.newFileSystem(uri, emptyMap<String, String>())
        }

    private fun load(stream: InputStream): List<V> {
        val content = InputStreamReader(stream)
        return gson.fromJson(content, valueType)
    }

    protected abstract fun GsonBuilder.setup()

    protected abstract fun V.overrides(older: V): Boolean
}

inline fun <reified T> typeToken(): Type = object : TypeToken<T>() {}.type

