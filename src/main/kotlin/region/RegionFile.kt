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

import com.intellij.util.io.LimitedInputStream
import java.io.*
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import net.jpountz.lz4.LZ4BlockInputStream

private const val SECTOR_SIZE = 4096
private const val CHUNKS_PER_REGION = 32 * 32

private fun xzToIndex(x: Int, z: Int) = x + z * 32
private fun indexToXz(index: Int) = (index.mod(32) to index.div(32))

/**
 * Helper class to read anvil region files (https://minecraft.wiki/w/Region_file_format)
 */
class RegionFile(private val filePath: File) : AutoCloseable {
    private val file = RandomAccessFile(filePath, "r")
    private val totalSectorCount = file.length() / SECTOR_SIZE
    private val chunkIndex = arrayOfNulls<ChunkIndexEntry>(CHUNKS_PER_REGION).apply {
        if (totalSectorCount < 2) {
            // Invalid format
            return@apply
        }

        val firstTwoSectors = ByteBuffer
            .wrap(ByteArray(2 * SECTOR_SIZE).apply {
                file.readFully(this)
            })
            .asIntBuffer()

        for (i in 0 until CHUNKS_PER_REGION) {
            val offsetAndSize = firstTwoSectors.get(i).toUInt()
            val entry = ChunkIndexEntry.decode(offsetAndSize, firstTwoSectors.get(CHUNKS_PER_REGION + i))
            if (entry.sectorOffset == 0 || entry.sectorCount == 0) {
                continue
            }

            this[i] = entry
        }
    }

    operator fun get(relativeChunkX: Int, relativeChunkZ: Int): Chunk? {
        if (!(relativeChunkX in 0..32 && relativeChunkZ in 0..32)) {
            throw IndexOutOfBoundsException("Chunk coordinates ($relativeChunkX, $relativeChunkZ) out of bounds for region file")
        }

        return chunkIndex[xzToIndex(relativeChunkX, relativeChunkZ)]?.let { entry ->
            Chunk(
                relativeChunkX,
                relativeChunkZ,
                entry
            )
        }
    }

    operator fun iterator(): Iterator<Chunk> = chunkIndex
        .asSequence()
        .mapIndexed { idx, e -> indexToXz(idx) to e }
        .filter { it.second != null }
        .map { (xz, e) -> Chunk(xz.first, xz.second, e!!) }
        .iterator()

    override fun close() {
        file.close()
    }

    internal data class ChunkIndexEntry(val sectorOffset: Int, val sectorCount: Int, val timestamp: Int) {
        companion object {
            fun decode(offsetAndSize: UInt, timestamp: Int): ChunkIndexEntry {
                val sectorOffset = offsetAndSize.shr(8).toInt()
                val sectorSize = offsetAndSize.and(0b11111111u).toInt()
                return ChunkIndexEntry(sectorOffset, sectorSize, timestamp)
            }
        }
    }

    inner class Chunk(
        val x: Int,
        val z: Int,
        val timestamp: Long,
        private val sectorOffset: Int,
        private val sectorCount: Int,
    ) {
        private val firstByte get() = (sectorOffset.toLong() * SECTOR_SIZE.toLong())
        val payloadLength: Int
            get() {
                file.seek(firstByte)
                return file.readInt()
            }

        internal constructor(x: Int, z: Int, chunkIndexEntry: ChunkIndexEntry) : this(
            x,
            z,
            chunkIndexEntry.timestamp.toLong(),
            chunkIndexEntry.sectorOffset,
            chunkIndexEntry.sectorCount,
        )

        fun read(): InputStream? {
            val payloadLength = payloadLength
            if (payloadLength > sectorCount * SECTOR_SIZE || (sectorOffset + sectorCount) > totalSectorCount) {
                return null
            }

            if (payloadLength == 0) {
                return InputStream.nullInputStream()
            }

            val chunkReader = BufferedInputStream(FileInputStream(filePath))
            chunkReader.skip(firstByte + 4)
            val payloadCompression = chunkReader.readNBytes(1)[0]
            val compressedPayload = LimitedInputStream(chunkReader, payloadLength - 1)

            return when (payloadCompression.toInt()) {
                // GZip
                1 -> GZIPInputStream(compressedPayload)
                // ZLib
                2 -> InflaterInputStream(compressedPayload, Inflater())
                // Uncompressed
                3 -> compressedPayload
                // LZ4
                4 -> LZ4BlockInputStream(compressedPayload)
                // Custom and/or yet-to-exist algorithm
                else -> null
            }
        }
    }
}
