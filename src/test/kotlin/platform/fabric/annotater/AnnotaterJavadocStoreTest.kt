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

import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocStore
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocTarget.ClassTarget
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocTarget.FieldTarget
import com.demonwav.mcdev.platform.fabric.annotater.javadoc.AnnotaterJavadocTarget.MethodTarget
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Annotater Javadoc Store Tests")
class AnnotaterJavadocStoreTest {
    @TempDir
    lateinit var mappings: Path

    private val mth = "net/minecraft/util/Mth"

    // The only file on annotater's 26.3 branch at the time of writing
    private val mthMapping = """
        |CLASS net/minecraft/util/Mth
        |	COMMENT Math utilities.
        |	METHOD clamp (III)I
        |		COMMENT Clamps `value` between `min` and `max` (inclusive).
        |		COMMENT
        |		COMMENT @param value the value to clamp
        |		COMMENT @param min the minimum value
        |		COMMENT @param max the maximum value
        |		COMMENT @return the clamped value
        |
    """.trimMargin()

    private fun writeMth(): Path {
        val file = mappings.resolve("$mth.mapping")
        file.parent.createDirectories()
        file.writeText(mthMapping)
        return file
    }

    @Test
    @DisplayName("Reads class and method comments")
    fun read() {
        writeMth()
        val store = AnnotaterJavadocStore(mappings)
        assertEquals("Math utilities.", store.get(ClassTarget(mth)))
        assertEquals(
            """
            Clamps `value` between `min` and `max` (inclusive).

            @param value the value to clamp
            @param min the minimum value
            @param max the maximum value
            @return the clamped value
            """.trimIndent(),
            store.get(MethodTarget(mth, "clamp", "(III)I")),
        )
        assertNull(store.get(MethodTarget(mth, "clamp", "(FFF)F")))
    }

    @Test
    @DisplayName("Rewriting unchanged comments produces an identical file")
    fun roundTrip() {
        val file = writeMth()
        val store = AnnotaterJavadocStore(mappings)
        store.set(ClassTarget(mth), store.get(ClassTarget(mth)))
        assertEquals(mthMapping, file.readText())
    }

    @Test
    @DisplayName("Adding a comment keeps the rest of the file")
    fun addComment() {
        val file = writeMth()
        val store = AnnotaterJavadocStore(mappings)
        store.set(FieldTarget(mth, "PI", "F"), "Pi as a float.")
        assertEquals(
            mthMapping.replace("\tMETHOD clamp", "\tFIELD PI F\n\t\tCOMMENT Pi as a float.\n\tMETHOD clamp"),
            file.readText(),
        )
        assertEquals("Pi as a float.", AnnotaterJavadocStore(mappings).get(FieldTarget(mth, "PI", "F")))
    }

    @Test
    @DisplayName("Removing the last comment deletes the file")
    fun removeComments() {
        val file = writeMth()
        val store = AnnotaterJavadocStore(mappings)
        store.set(MethodTarget(mth, "clamp", "(III)I"), null)
        assertEquals("CLASS net/minecraft/util/Mth\n\tCOMMENT Math utilities.\n", file.readText())
        store.set(ClassTarget(mth), "")
        assertFalse(file.exists())
    }

    @Test
    @DisplayName("Inner classes are stored in their outer class's file")
    fun innerClass() {
        val store = AnnotaterJavadocStore(mappings)
        val outer = "net/minecraft/world/level/Level"
        store.set(ClassTarget("$outer\$Inner"), "An inner class.")
        store.set(ClassTarget(outer), "The level.")
        // Enigma nests inner classes inside their outer class
        assertEquals(
            "CLASS $outer\n\tCOMMENT The level.\n\tCLASS Inner\n\t\tCOMMENT An inner class.\n",
            mappings.resolve("$outer.mapping").readText(),
        )
        assertEquals("An inner class.", AnnotaterJavadocStore(mappings).get(ClassTarget("$outer\$Inner")))
    }

    @Test
    @DisplayName("Outer class names")
    fun outerClassName() {
        assertEquals("a/B", ClassTarget("a/B\$C\$D").outerClass)
        assertEquals("a/\$B", ClassTarget("a/\$B").outerClass)
    }
}
