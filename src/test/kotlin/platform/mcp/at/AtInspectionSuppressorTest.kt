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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.testInspectionFix
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Transformer Inspection Suppressor Tests")
class AtInspectionSuppressorTest : BaseMinecraftTest() {

    @Test
    @DisplayName("Entry-Level Suppress")
    fun entryLevelSuppress() {
        fixture.configureByText(
            "test_at.cfg",
            """
            public Unresolved # Suppress:AtUnresolvedReference
            public <error descr="Cannot resolve symbol 'Unresolved'">Unresolved</error>
            """.trimIndent()
        )

        fixture.enableInspections(AtUnresolvedReferenceInspection::class.java)
        fixture.checkHighlighting()
    }

    @Test
    @DisplayName("Entry-Level Suppress Fix")
    fun entryLevelSuppressFix() {
        fixture.enableInspections(AtUnresolvedReferenceInspection::class.java)
        testInspectionFix(
            fixture,
            "Suppress AtUnresolvedReference for entry",
            AtFileType,
            "public <caret>Unresolved",
            "public Unresolved # Suppress:AtUnresolvedReference"
        )
    }

    @Test
    @DisplayName("File-Level Suppress")
    fun fileLevelSuppress() {
        fixture.configureByText(
            "test_at.cfg",
            """
            # Suppress:AtUnresolvedReference
            public Unresolved
            public Unresolved
            """.trimIndent()
        )

        fixture.enableInspections(AtUnresolvedReferenceInspection::class.java)
        fixture.checkHighlighting()
    }

    @Test
    @DisplayName("File-Level Suppress Fix With No Existing Comments")
    fun fileLevelSuppressFixNoComments() {
        fixture.enableInspections(AtUnresolvedReferenceInspection::class.java)
        testInspectionFix(
            fixture,
            "Suppress AtUnresolvedReference for file",
            AtFileType,
            "public <caret>Unresolved",
            """
            # Suppress:AtUnresolvedReference
            public Unresolved
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("File-Level Suppress Fix With Unrelated Comment")
    fun fileLevelSuppressFixWithUnrelatedComment() {
        fixture.enableInspections(AtUnresolvedReferenceInspection::class.java)
        testInspectionFix(
            fixture,
            "Suppress AtUnresolvedReference for file",
            AtFileType,
            """
            # This is a header comment
            public <caret>Unresolved
            """.trimIndent(),
            """
            # This is a header comment
            # Suppress:AtUnresolvedReference
            public Unresolved
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("File-Level Suppress Fix With Existing Suppress")
    fun fileLevelSuppressFixWithExistingSuppress() {
        fixture.enableInspections(AtUnresolvedReferenceInspection::class.java)
        testInspectionFix(
            fixture,
            "Suppress AtUnresolvedReference for file",
            AtFileType,
            """
            # This is a header comment
            # Suppress:AtUsage
            public <caret>Unresolved
            """.trimIndent(),
            """
            # This is a header comment
            # Suppress:AtUsage,AtUnresolvedReference
            public Unresolved
            """.trimIndent()
        )
    }
}
