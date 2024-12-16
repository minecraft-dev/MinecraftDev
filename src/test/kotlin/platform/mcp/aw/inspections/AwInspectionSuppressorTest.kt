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
import com.demonwav.mcdev.framework.testInspectionFix
import com.demonwav.mcdev.platform.mcp.aw.AwFileType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Widener Inspection Suppressor Tests")
class AwInspectionSuppressorTest : BaseMinecraftTest() {

    @Test
    @DisplayName("Entry-Level Suppress")
    fun entryLevelSuppress() {
        fixture.configureByText(
            "test.accesswidener",
            """
            accessible class Unresolved # Suppress:AwUnresolvedReference
            accessible class <error descr="Cannot resolve symbol 'Unresolved'">Unresolved</error>
            """.trimIndent()
        )

        fixture.enableInspections(AwUnresolvedReferenceInspection::class.java)
        fixture.checkHighlighting()
    }

    @Test
    @DisplayName("Entry-Level Suppress Fix")
    fun entryLevelSuppressFix() {
        fixture.enableInspections(AwUnresolvedReferenceInspection::class.java)
        testInspectionFix(
            fixture,
            "Suppress AwUnresolvedReference for entry",
            AwFileType,
            "accessible class <caret>Unresolved",
            "accessible class Unresolved # Suppress:AwUnresolvedReference"
        )
    }

    @Test
    @DisplayName("File-Level Suppress")
    fun fileLevelSuppress() {
        fixture.configureByText(
            "test.accesswidener",
            """
            # Suppress:AwUnresolvedReference
            accessible class Unresolved
            accessible class Unresolved
            """.trimIndent()
        )

        fixture.enableInspections(AwUnresolvedReferenceInspection::class.java)
        fixture.checkHighlighting()
    }

    @Test
    @DisplayName("File-Level Suppress Fix With No Existing Comments")
    fun fileLevelSuppressFixNoComments() {
        fixture.enableInspections(AwUnresolvedReferenceInspection::class.java)
        testInspectionFix(
            fixture,
            "Suppress AwUnresolvedReference for file",
            AwFileType,
            "accessible class <caret>Unresolved",
            """
            # Suppress:AwUnresolvedReference
            accessible class Unresolved
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("File-Level Suppress Fix With Unrelated Comment")
    fun fileLevelSuppressFixWithUnrelatedComment() {
        fixture.enableInspections(AwUnresolvedReferenceInspection::class.java)
        testInspectionFix(
            fixture,
            "Suppress AwUnresolvedReference for file",
            AwFileType,
            """
            # This is a header comment
            accessible class <caret>Unresolved
            """.trimIndent(),
            """
            # This is a header comment
            # Suppress:AwUnresolvedReference
            accessible class Unresolved
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("File-Level Suppress Fix With Existing Suppress")
    fun fileLevelSuppressFixWithExistingSuppress() {
        fixture.enableInspections(AwUnresolvedReferenceInspection::class.java)
        testInspectionFix(
            fixture,
            "Suppress AwUnresolvedReference for file",
            AwFileType,
            """
            # This is a header comment
            # Suppress:AwUsage
            accessible class <caret>Unresolved
            """.trimIndent(),
            """
            # This is a header comment
            # Suppress:AwUsage,AwUnresolvedReference
            accessible class Unresolved
            """.trimIndent()
        )
    }
}
