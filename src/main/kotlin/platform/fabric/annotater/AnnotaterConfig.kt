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

import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * The contents of an annotater checkout's `config.json`.
 */
data class AnnotaterConfig(
    val version: String,
    val mappings: String,
    val unpickDefinitions: String?,
) {
    companion object {
        const val FILE_NAME = "config.json"

        private class Raw(
            val version: String? = null,
            val mappings: String? = null,
            val unpickDefinitions: String? = null,
        )

        @Throws(IOException::class)
        fun read(root: Path): AnnotaterConfig {
            val file = root.resolve(FILE_NAME)
            if (!file.exists()) {
                throw IOException("$root does not contain a $FILE_NAME file")
            }
            val raw = try {
                Gson().fromJson(file.readText(), Raw::class.java)
            } catch (e: JsonParseException) {
                throw IOException("Invalid $FILE_NAME: ${e.message}", e)
            } ?: throw IOException("$FILE_NAME is empty")

            val version = raw.version?.takeIf { it.isNotBlank() }
                ?: throw IOException("$FILE_NAME is missing \"version\"")
            val mappings = validatePath(raw.mappings ?: throw IOException("$FILE_NAME is missing \"mappings\""))
            val unpick = raw.unpickDefinitions?.let(::validatePath)
            return AnnotaterConfig(version, mappings, unpick)
        }

        /**
         * Paths must be relative subdirectories using forward slashes, like mcsrc requires.
         */
        @Throws(IOException::class)
        private fun validatePath(path: String): String {
            val segments = path.split('/')
            val valid = path.isNotEmpty() &&
                !path.startsWith('/') &&
                '\\' !in path &&
                ':' !in path &&
                segments.none { it.isEmpty() || it == "." || it == ".." }
            if (!valid) {
                throw IOException("Invalid path in $FILE_NAME: \"$path\"")
            }
            return path
        }
    }
}
