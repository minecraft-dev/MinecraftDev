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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Transformer Duplicate Entry Inspection Tests")
class AtDuplicateEntryInspectionTest : BaseMinecraftTest() {

    @Test
    @DisplayName("Duplicate Entries")
    fun duplicateEntries() {
        buildProject {
            at(
                "test_at.cfg",
                """
                public test.value.UniqueClass
                <warning descr="Duplicate entry">public test.value.DuplicateClass</warning>
                <warning descr="Duplicate entry">public test.value.DuplicateClass</warning>

                public test.value.UniqueClass *
                <warning descr="Duplicate entry">public test.value.DuplicateClass *</warning>
                <warning descr="Duplicate entry">public test.value.DuplicateClass *</warning>

                public test.value.UniqueClass *()
                <warning descr="Duplicate entry">public test.value.DuplicateClass *()</warning>
                <warning descr="Duplicate entry">public test.value.DuplicateClass *()</warning>

                public test.value.UniqueClass field
                <warning descr="Duplicate entry">public test.value.DuplicateClass field</warning>
                <warning descr="Duplicate entry">public test.value.DuplicateClass field</warning>

                public test.value.UniqueClass method()V
                <warning descr="Duplicate entry">public test.value.DuplicateClass method()V</warning>
                <warning descr="Duplicate entry">public test.value.DuplicateClass method()V</warning>

                public test.value.UniqueClass method(II)V
                <warning descr="Duplicate entry">public test.value.DuplicateClass method(II)V</warning>
                <warning descr="Duplicate entry">public test.value.DuplicateClass method(II)V</warning>
                """.trimIndent()
            )
        }

        fixture.enableInspections(AtDuplicateEntryInspection::class.java)
        fixture.checkHighlighting()
    }
}
