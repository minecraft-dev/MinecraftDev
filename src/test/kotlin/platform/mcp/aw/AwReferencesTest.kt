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

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Widener References Tests")
class AwReferencesTest : BaseMinecraftTest(PlatformType.MCP, PlatformType.FABRIC) {

    @BeforeEach
    fun setupProject() {
        buildProject {
            java(
                "com/demonwav/mcdev/mcp/test/TestLibrary.java",
                """
                package com.demonwav.mcdev.mcp.test;
                public class TestLibrary {
                    private String privString;
                    private void method() {}
                    private void overloaded() {}
                    private void overloaded(String arg) {}
                    private void copy(TestLibrary from) {}
                    private void add(int amount) {}
                }
                """.trimIndent()
            )
        }
    }

    private inline fun <reified E : PsiElement> testReferenceAtCaret(
        @Language("Access Widener") at: String,
        crossinline test: (element: E) -> Unit
    ) {
        fixture.configureByText("test.accesswidener", at)
        runReadAction {
            val ref = fixture.getReferenceAtCaretPositionWithAssertion()
            val resolved = ref.resolve().also(::assertNotNull)!!
            test(assertInstanceOf(E::class.java, resolved))
        }
    }

    @Test
    @DisplayName("Class Reference")
    fun classReference() {
        testReferenceAtCaret<PsiClass>("accessible field com/demonwav/mcdev/mcp/test/<caret>TestLibrary privString Ljava/lang/String;") { clazz ->
            val expectedClass = fixture.findClass("com.demonwav.mcdev.mcp.test.TestLibrary")
            assertEquals(expectedClass, clazz)
        }
    }

    @Test
    @DisplayName("Field Reference")
    fun fieldReference() {
        testReferenceAtCaret<PsiField>("accessible field com/demonwav/mcdev/mcp/test/TestLibrary <caret>privString Ljava/lang/String;") { field ->
            val expectedClass = fixture.findClass("com.demonwav.mcdev.mcp.test.TestLibrary")
            val expectedField = expectedClass.findFieldByName("privString", false)
            assertEquals(expectedField, field)
        }
    }

    @Test
    @DisplayName("Method Reference")
    fun methodReference() {
        testReferenceAtCaret<PsiMethod>("accessible method com/demonwav/mcdev/mcp/test/TestLibrary <caret>method ()V") { method ->
            val expectedClass = fixture.findClass("com.demonwav.mcdev.mcp.test.TestLibrary")
            val expectedMethod = expectedClass.findMethodsByName("method", false).single()
            assertEquals(expectedMethod, method)
        }
    }

    @Test
    @DisplayName("Method Overload Reference")
    fun methodOverloadReference() {
        testReferenceAtCaret<PsiMethod>(
            "accessible method com/demonwav/mcdev/mcp/test/TestLibrary <caret>overloaded ()V"
        ) { method ->
            val expectedClass = fixture.findClass("com.demonwav.mcdev.mcp.test.TestLibrary")
            val expectedMethod = expectedClass.findMethodsByName("overloaded", false).single { !it.hasParameters() }
            assertEquals(expectedMethod, method)
        }

        testReferenceAtCaret<PsiMethod>(
            "accessible method com/demonwav/mcdev/mcp/test/TestLibrary <caret>overloaded (Ljava/lang/String;)V"
        ) { method ->
            val expectedClass = fixture.findClass("com.demonwav.mcdev.mcp.test.TestLibrary")
            val expectedMethod = expectedClass.findMethodsByName("overloaded", false).single { it.hasParameters() }
            assertEquals(expectedMethod, method)
        }
    }

    @Test
    @DisplayName("Descriptor Class Type Reference")
    fun descriptorClassTypeReference() {
        testReferenceAtCaret<PsiClass>(
            "accessible method com/demonwav/mcdev/mcp/test/TestLibrary copy (Ljava/lang/<caret>String;)V"
        ) { clazz ->
            val expectedClass = fixture.findClass(CommonClassNames.JAVA_LANG_STRING)
            assertEquals(expectedClass, clazz)
        }
    }
}
