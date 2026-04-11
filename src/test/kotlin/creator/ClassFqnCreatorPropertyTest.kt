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

import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.custom.model.BuildSystemCoordinates
import com.demonwav.mcdev.creator.custom.model.ClassFqn
import com.intellij.openapi.ui.validation.invoke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Class FQN Creator Property Tests")
class ClassFqnCreatorPropertyTest : CreatorTemplateProcessorTestBase() {

    private fun checkValidation(input: String, expectedMessage: String?) {
        val validation = BuiltinValidations.validClassFqn { input }
        val validationInfo = validation.validate()
        if (expectedMessage == null) {
            assertNull(validationInfo?.message) { "Expected input to be valid: '$input'" }
        } else {
            assertNotNull(validationInfo, "Expected input to be invalid: '$input'")
            assertEquals(expectedMessage, validationInfo!!.message)
        }
    }

    @Test
    @DisplayName("Validation")
    fun validation() {
        checkValidation("test.TestName", null)
        val invalidFqns = listOf(
            "test.0InvalidName",
            "test.Invalid-Name",
            "test.package.InvalidPackage",
            "test..InvalidPackage",
            "test."
        )
        for (fqn in invalidFqns) {
            checkValidation(fqn, "Must be a valid class fully qualified name")
        }
    }

    @Test
    @DisplayName("Class Name Suggestion")
    fun classNameSuggestion() {
        makeTemplate(
            """
            {
              "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
              "properties": [
                {
                  "name": "BUILD_COORDS",
                  "type": "build_system_coordinates"
                },
                {
                  "name": "NAME",
                  "type": "string"
                },
                {
                  "name": "FQN",
                  "type": "class_fqn",
                  "derives": {
                    "method": "suggestClassName",
                    "parents": ["BUILD_COORDS", "NAME"]
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val buildCoordsProperty = processor.context.property<BuildSystemCoordinates>("BUILD_COORDS")
        val nameProperty = processor.context.property<String>("NAME")
        val fqnProperty = processor.context.property<ClassFqn>("FQN")

        buildCoordsProperty.graphProperty.set(BuildSystemCoordinates("com.example.project", "example-project", "1.0"))
        nameProperty.graphProperty.set("My Project")
        assertEquals(ClassFqn("com.example.project.myProject.MyProject"), fqnProperty.get())
    }
}
