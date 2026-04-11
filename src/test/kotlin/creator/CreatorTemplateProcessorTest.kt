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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Creator Template Processor Tests")
class CreatorTemplateProcessorTest : CreatorTemplateProcessorTestBase() {

    @Test
    @DisplayName("Duplicate Property Name")
    fun duplicatePropertyName() {
        val exception = assertThrows<TemplateValidationException> {
            makeTemplate(
                """
                {
                  "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
                  "properties": [
                    {
                      "name": "PROP",
                      "type": "string"
                    },
                    {
                      "name": "PROP",
                      "type": "string"
                    }
                  ]
                }
                """.trimIndent()
            )
        }
        assertEquals("Duplicate property name PROP", exception.message?.replace("\r\n", "\n"))
    }

    @Test
    @DisplayName("Unknown Property Type")
    fun unknownPropertyType() {
        val exception = assertThrows<TemplateValidationException> {
            makeTemplate(
                """
                {
                  "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
                  "properties": [
                    {
                      "name": "PROP",
                      "type": "bad_type"
                    }
                  ]
                }
                """.trimIndent()
            )
        }
        assertEquals("Unknown template property type bad_type", exception.message)
    }
}
