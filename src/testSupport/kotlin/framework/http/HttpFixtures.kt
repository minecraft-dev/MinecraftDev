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

package com.demonwav.mcdev.framework.http

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

/**
 * Fixture roots and the files in them. A root uses WireMock's layout: stub mappings under [MAPPINGS_DIR] and the
 * bodies they point at under [FILES_DIR], both named by [HttpFixturePaths].
 *
 * Functions that take a URL throw [IllegalArgumentException] for anything that isn't an absolute HTTP(S) URL.
 */
object HttpFixtures {
    /**
     * System property holding the fixture roots, separated by the platform path separator.
     */
    const val ROOTS_PROPERTY = "mcdev.test.httpFixtures"

    const val MAPPINGS_DIR = "mappings"
    const val FILES_DIR = "__files"

    private const val CLASSPATH_ROOT = "http-fixtures"

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    /**
     * The roots from [ROOTS_PROPERTY], or the `http-fixtures` directory on the classpath if the property is unset, or
     * an empty list if neither is available.
     */
    fun defaultRoots(): List<Path> {
        val property = System.getProperty(ROOTS_PROPERTY)?.takeIf { it.isNotBlank() }
        if (property != null) {
            return property.split(File.pathSeparatorChar).filter { it.isNotBlank() }.map { Paths.get(it) }
        }
        val resource = HttpFixtures::class.java.classLoader.getResource(CLASSPATH_ROOT)
        if (resource != null && resource.protocol == "file") {
            return listOf(Paths.get(resource.toURI()))
        }
        return emptyList()
    }

    /**
     * The mapping file for [url], relative to a root.
     */
    fun mappingFileName(url: String): String = "$MAPPINGS_DIR/${HttpFixturePaths.pathFor(url)}.json"

    /**
     * The body file for [url], relative to the root's [FILES_DIR], which is what a mapping's `bodyFileName` holds.
     */
    fun bodyFileName(url: String): String = HttpFixturePaths.pathFor(url)

    fun mappingPath(root: Path, url: String): Path = root.resolve(mappingFileName(url))

    fun bodyPath(root: Path, url: String): Path = root.resolve(FILES_DIR).resolve(bodyFileName(url))

    /**
     * Whether any of [roots] has a mapping for [url] at [mappingFileName]. Mappings saved under other names aren't
     * found.
     */
    fun exists(roots: List<Path>, url: String): Boolean = roots.any { mappingPath(it, url).isRegularFile() }

    /**
     * Writes the mapping for [url] under [root], and its body if there is one. A null [body] deletes any existing body
     * file. Throws [IllegalStateException] if another fixture's file is in the way.
     */
    fun write(root: Path, url: String, status: Int, headers: Map<String, String>, body: ByteArray?) {
        val mapping = mappingPath(root, url)
        val bodyFile = bodyPath(root, url)
        prepare(root, mapping, url)
        mapping.writeText(mappingJson(url, status, headers, hasBody = body != null))
        if (body != null) {
            prepare(root, bodyFile, url)
            bodyFile.writeBytes(body)
        } else {
            Files.deleteIfExists(bodyFile)
        }
    }

    /**
     * The mapping JSON [write] saves. The same arguments always give the same text.
     */
    fun mappingJson(url: String, status: Int, headers: Map<String, String>, hasBody: Boolean): String {
        val json = JsonObject()
        json.addProperty("name", url)
        json.add(
            "request",
            JsonObject().apply {
                addProperty("method", "GET")
                addProperty("url", HttpFixturePaths.mockPath(url))
            },
        )
        json.add(
            "response",
            JsonObject().apply {
                addProperty("status", status)
                if (hasBody) {
                    addProperty("bodyFileName", bodyFileName(url))
                }
                if (headers.isNotEmpty()) {
                    add("headers", JsonObject().apply { headers.toSortedMap().forEach(::addProperty) })
                }
            },
        )
        return gson.toJson(json) + "\n"
    }

    // Fail clearly when a path is needed as both a file and a directory
    private fun prepare(root: Path, file: Path, url: String) {
        var dir: Path? = file.parent
        while (dir != null && dir != root) {
            check(!dir.exists() || dir.isDirectory()) { "Fixture for $url needs $dir to be a directory" }
            dir = dir.parent
        }
        check(!file.isDirectory()) { "Fixture for $url would overwrite the directory $file" }
        Files.createDirectories(file.parent)
    }
}
