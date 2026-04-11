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
import com.demonwav.mcdev.creator.custom.TemplateValidationItem
import com.demonwav.mcdev.util.firstOfType
import kotlin.collections.singleOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("String Creator Property Tests")
class StringCreatorPropertyTest : CreatorTemplateProcessorTestBase() {

    @Test
    @DisplayName("Invalid Validator")
    fun invalidValidator() {
        val reporter = makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "STRING",
                  "type": "string",
                  "validator": "[invalid"
                }
              ]
            }
            """.trimIndent()
        )

        assertTrue(reporter.hasErrors)
        assertEquals(
            "Invalid validator regex: '[invalid': Unclosed character class near index 7\n[invalid\n       ^",
            reporter.items["STRING"]?.singleOrNull()?.message?.replace("\r\n", "\n")
        )
    }

    @Test
    @DisplayName("Replace Derivation")
    fun replaceDerivation() {
        makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "STRING",
                  "type": "string",
                  "derives": {
                    "parents": ["PROJECT_NAME"],
                    "method": "replace",
                    "parameters": {
                      "regex": "[^a-z0-9-_]+",
                      "replacement": "_",
                      "maxLength": 32,
                      "lowercase": true
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val projectNameProperty = processor.context.property<String>("PROJECT_NAME")
        val stringProperty = processor.context.property<String>("STRING")

        projectNameProperty.graphProperty.set("Sanitize This")
        assertEquals("sanitize_this", stringProperty.get())

        projectNameProperty.graphProperty.set("This string will get truncated at some point")
        assertEquals("this_string_will_get_truncated_a", stringProperty.get())
    }

    @Test
    @DisplayName("Replace Derivation Missing Parameters")
    fun replaceDerivationMissingParameters() {
        val reporter = makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "STRING",
                  "type": "string",
                  "derives": {
                    "parents": ["PROJECT_NAME"],
                    "method": "replace"
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Missing parameters", reporter.items["STRING"]?.singleOrNull()?.message)
    }

    @Test
    @DisplayName("Replace Derivation Missing Parent Value")
    fun replaceDerivationMissingParentValue() {
        val reporter = makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "STRING",
                  "type": "string",
                  "derives": {
                    "parents": [],
                    "method": "replace",
                    "parameters": {}
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Missing parent value", reporter.items["STRING"]?.singleOrNull()?.message)
    }

    @Test
    @DisplayName("Replace Derivation More Than One Parent Defined")
    fun replaceDerivationMoreThanOneParentDefined() {
        val reporter = makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "STRING",
                  "type": "string",
                  "derives": {
                    "parents": ["PROJECT_NAME", "PROJECT_NAME"],
                    "method": "replace",
                    "parameters": {}
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(
            "More than one parent defined",
            reporter.items["STRING"]?.firstOfType<TemplateValidationItem.Warn>()?.message
        )
    }

    @Test
    @DisplayName("Replace Derivation Parent Property Must Produce A String Value")
    fun replaceDerivationParentPropertyMustProduceAStringValue() {
        val reporter = makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "BOOL",
                  "type": "boolean"
                },
                {
                  "name": "STRING",
                  "type": "string",
                  "derives": {
                    "parents": ["BOOL"],
                    "method": "replace",
                    "parameters": {}
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Parent property must produce a string value", reporter.items["STRING"]?.singleOrNull()?.message)
    }

    @Test
    @DisplayName("Replace Derivation Missing Regex Parameter")
    fun replaceDerivationMissingRegexParameter() {
        val reporter = makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "STRING",
                  "type": "string",
                  "derives": {
                    "parents": ["PROJECT_NAME"],
                    "method": "replace",
                    "parameters": {
                      "replacement": "_"
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Missing 'regex' string parameter", reporter.items["STRING"]?.singleOrNull()?.message)
    }

    @Test
    @DisplayName("Replace Derivation Missing Replacement Parameter")
    fun replaceDerivationMissingReplacementParameter() {
        val reporter = makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "STRING",
                  "type": "string",
                  "derives": {
                    "parents": ["PROJECT_NAME"],
                    "method": "replace",
                    "parameters": {
                      "regex": "[^a-z0-9-_]+"
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Missing 'replacement' string parameter", reporter.items["STRING"]?.singleOrNull()?.message)
    }

    @Test
    @DisplayName("Select Derivation")
    fun selectDerivation() {
        makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "STRING",
                  "type": "string",
                  "derives": {
                    "parents": ["PROJECT_NAME"],
                    "select": [
                      { "condition": "${'$'}PROJECT_NAME == 'Name 1'", "value": "Value 1" },
                      { "condition": "${'$'}PROJECT_NAME == 'Name 2'", "value": "Value 2" },
                      { "condition": "${'$'}PROJECT_NAME == 'Name 3'", "value": "Value 3" }
                    ],
                    "default": "Default value"
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val projectNameProperty = processor.context.property<String>("PROJECT_NAME")
        val stringProperty = processor.context.property<String>("STRING")

        projectNameProperty.graphProperty.set("Name 1")
        assertEquals("Value 1", stringProperty.get())

        projectNameProperty.graphProperty.set("Name 2")
        assertEquals("Value 2", stringProperty.get())

        projectNameProperty.graphProperty.set("Name 3")
        assertEquals("Value 3", stringProperty.get())

        projectNameProperty.graphProperty.set("Name 4")
        assertEquals("Default value", stringProperty.get())
    }

    @Test
    @DisplayName("Select Derivation With Modification")
    fun selectDerivationWithModification() {
        makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "STRING",
                  "type": "string",
                  "derives": {
                    "parents": ["PROJECT_NAME"],
                    "select": [
                      { "condition": "${'$'}PROJECT_NAME == 'Name 1'", "value": "Value 1" },
                      { "condition": "${'$'}PROJECT_NAME == 'Name 2'", "value": "Value 2" }
                    ],
                    "default": "Default value",
                    "whenModified": true
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val projectNameProperty = processor.context.property<String>("PROJECT_NAME")
        val stringProperty = processor.context.property<String>("STRING")

        projectNameProperty.graphProperty.set("Name 1")
        assertEquals("Value 1", stringProperty.get())

        stringProperty.graphProperty.set("Custom value")
        projectNameProperty.graphProperty.set("Name 2")
        assertEquals("Custom value", stringProperty.get())
    }
}
