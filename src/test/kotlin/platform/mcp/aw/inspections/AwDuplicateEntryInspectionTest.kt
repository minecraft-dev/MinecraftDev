/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.aw.inspections

import com.demonwav.mcdev.framework.BaseMinecraftTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Widener Duplicate Entry Inspection Tests")
class AwDuplicateEntryInspectionTest : BaseMinecraftTest() {

    @Test
    @DisplayName("Duplicate Entries")
    fun duplicateEntries() {
        buildProject {
            aw(
                "test.accesswidener",
                """
                accessible class test/value/UniqueClass
                <warning descr="Duplicate entry">accessible class test/value/DuplicateClass</warning>
                <warning descr="Duplicate entry">accessible class test/value/DuplicateClass</warning>

                accessible field test/value/UniqueClass field I
                <warning descr="Duplicate entry">accessible field test/value/DuplicateClass field I</warning>
                <warning descr="Duplicate entry">accessible field test/value/DuplicateClass field I</warning>

                accessible method test/value/UniqueClass method()V
                <warning descr="Duplicate entry">accessible method test/value/DuplicateClass method()V</warning>
                <warning descr="Duplicate entry">accessible method test/value/DuplicateClass method()V</warning>

                accessible method test/value/UniqueClass method(II)V
                <warning descr="Duplicate entry">accessible method test/value/DuplicateClass method(II)V</warning>
                <warning descr="Duplicate entry">accessible method test/value/DuplicateClass method(II)V</warning>
                """.trimIndent()
            )
        }

        fixture.enableInspections(DuplicateAwEntryInspection::class.java)
        fixture.checkHighlighting()
    }
}
