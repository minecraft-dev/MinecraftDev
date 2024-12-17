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
import com.demonwav.mcdev.framework.assertEqualsUnordered
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.aw.AwElementFactory.Access
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.application.runWriteActionAndWait
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Widener Completion Tests")
class AwCompletionTest : BaseMinecraftTest(PlatformType.MCP, PlatformType.FABRIC) {

    @BeforeEach
    fun setupProject() {
        buildProject {
            java(
                "net/minecraft/Minecraft.java",
                """
                package net.minecraft;
                public class Minecraft {
                    private String privString;
                    private void method() {}
                    private void overloaded() {}
                    private void overloaded(String arg) {}
                    private void copy(TestLibrary from) {}
                    private void add(int amount) {}
                }
                """.trimIndent()
            )
            java(
                "net/minecraft/server/MinecraftServer.java",
                """
                package net.minecraft.server;
                public class MinecraftServer {}
                """.trimIndent()
            )
        }
    }

    private fun doCompletionTest(
        @Language("Access Widener") before: String,
        @Language("Access Widener") after: String,
        lookupToUse: String? = null
    ) {
        fixture.configureByText("test.accesswidener", before)
        fixture.completeBasic()
        if (lookupToUse != null) {
            val lookupElement = fixture.lookupElements?.find { it.lookupString == lookupToUse }
            assertNotNull(lookupElement, "Could not find lookup element with lookup string '$lookupToUse'")
            runWriteActionAndWait {
                fixture.lookup.currentItem = lookupElement
            }
            fixture.type(Lookup.NORMAL_SELECT_CHAR)
        }
        fixture.checkResult(after)
    }

    @Test
    @DisplayName("Header Lookup Elements In Empty File")
    fun headerLookupElements() {
        fixture.configureByText("test.accesswidener", "<caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings = setOf("accessWidener v1 named", "accessWidener v2 named")
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Access Lookup Elements")
    fun accessLookupElements() {
        fixture.configureByText("test.accesswidener", "accessWidener v2 named\n<caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings = Access.entries.map { it.text }
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Target Kind Lookup Elements")
    fun targetKindLookupElements() {
        fixture.configureByText("test.accesswidener", "accessible <caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings = listOf("class", "method", "field")
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Field Lookup Elements")
    fun fieldLookupElements() {
        fixture.configureByText("test.accesswidener", "accessible field net/minecraft/Minecraft <caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings = setOf("privString Ljava/lang/String;")
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Method Lookup Elements")
    fun methodLookupElements() {
        fixture.configureByText("test.accesswidener", "accessible method net/minecraft/Minecraft <caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings = setOf("add (I)V", "copy (L;)V", "method ()V", "overloaded ()V", "overloaded (Ljava/lang/String;)V")
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Field Name Completion")
    fun fieldNameCompletion() {
        doCompletionTest(
            "accessible field net/minecraft/Minecraft privS<caret>",
            "accessible field net/minecraft/Minecraft privString Ljava/lang/String;"
        )
    }

    @Test
    @DisplayName("Method Name Completion")
    fun methodNameCompletion() {
        doCompletionTest(
            "accessible method net/minecraft/Minecraft add<caret>",
            "accessible method net/minecraft/Minecraft add (I)V"
        )
    }

    @Test
    @DisplayName("Method Name Completion Cleaning End Of Line")
    fun methodNameCompletionCleaningEndOfLine() {
        doCompletionTest(
            "accessible method net/minecraft/Minecraft overload<caret>ed (Ljava/some)V invalid; stuff",
            "accessible method net/minecraft/Minecraft overloaded (Ljava/lang/String;)V",
            "overloaded (Ljava/lang/String;)V"
        )
    }
}
