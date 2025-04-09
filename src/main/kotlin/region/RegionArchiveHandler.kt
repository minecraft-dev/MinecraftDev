/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.region

import com.intellij.openapi.vfs.impl.ArchiveHandler
import java.io.FileNotFoundException
import java.io.InputStream

private val COORDINATE_FILE_NAME_REGEX = Regex("""^[a-z]\.(?<x>-?\d+)\.(?<z>-?\d+)\.\w+${'$'}""")

/**
 * Parses the coordinates of a region/chunk file (e.g. `r.1.-1.mca`), if representable by Int
 */
private fun parseFileNameCoordinates(fileName: CharSequence): Pair<Int, Int>? = COORDINATE_FILE_NAME_REGEX
    .matchAt(fileName, 0)
    ?.let { matchResult ->
        val (x, z) = matchResult.destructured
        return try {
            Pair(x.toInt(), z.toInt())
        } catch (e: NumberFormatException) {
            // This may happen if the file name contains a number that's too big for an Int
            null
        }
    }

class RegionArchiveHandler(path: String) : ArchiveHandler(path) {
    private val regionFile = RegionFile(file)

    // Absolute region coordinates. May be null if the file has a nonstandard name.
    private val regionXZ = parseFileNameCoordinates(path.substringAfterLast('/'))

    override fun createEntriesMap() = mutableMapOf<String, EntryInfo>().apply {
        val root = createRootEntry()
        this[""] = root

        for (chunk in regionFile) {
            val name = if (regionXZ != null) {
                val x = chunk.x + regionXZ.first * 32
                val z = chunk.z + regionXZ.second * 32
                "c.$x.$z.nbt"
            } else {
                val x = chunk.x
                val z = chunk.z
                "c.~$x.~$z.nbt"
            }

            this[name] = EntryInfo(name, false, chunk.payloadLength.toLong(), chunk.timestamp, root)
        }
    }

    fun resolveChunk(relativePath: String): RegionFile.Chunk? {
        var (x, z) = parseFileNameCoordinates(relativePath.substringAfterLast('/'))
            ?: throw FileNotFoundException("Illegal name for region file entry: $relativePath")

        x = x.mod(32)
        z = z.mod(32)
        return regionFile[x, z]
    }

    override fun getInputStream(relativePath: String): InputStream {
        val stream = resolveChunk(relativePath)?.read()

        if (stream == null) {
            throw FileNotFoundException("Chunk entry is not initialized")
        } else {
            return stream
        }
    }

    override fun contentsToByteArray(relativePath: String) = getInputStream(relativePath).readBytes()
}
