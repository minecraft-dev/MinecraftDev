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

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.assertEqualsUnordered
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.AtElementFactory.Keyword
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.application.runWriteActionAndWait
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Transformer Completion Tests")
class AtCompletionTest : BaseMinecraftTest(PlatformType.MCP, PlatformType.NEOFORGE) {

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

        // Force 1.20.2 because we test the non-SRG member names with NeoForge
        MinecraftFacet.getInstance(fixture.module, McpModuleType)!!
            .updateSettings(McpModuleSettings.State(minecraftVersion = "1.20.2"))
    }

    private fun doCompletionTest(
        @Language("Access Transformers") before: String,
        @Language("Access Transformers") after: String,
        lookupToUse: String? = null
    ) {
        fixture.configureByText("test_at.cfg", before)
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
    @DisplayName("Keyword Lookup Elements In Empty File")
    fun keywordLookupElements() {
        fixture.configureByText("test_at.cfg", "<caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings = Keyword.entries.map { it.text }
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Empty Class Name Lookup Elements")
    fun emptyClassNameLookupElements() {
        fixture.configureByText("test_at.cfg", "public <caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings = setOf("net.minecraft.Minecraft", "net.minecraft.server.MinecraftServer")
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Class Name Package Lookup Elements")
    fun packageLookupElements() {
        fixture.configureByText("test_at.cfg", "public net.<caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings = setOf("minecraft")
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Class Name Package And Class Lookup Elements")
    fun packageAndClassLookupElements() {
        fixture.configureByText("test_at.cfg", "public net.minecraft.<caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings = setOf("server", "Minecraft")
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Member Lookup Elements")
    fun memberLookupElements() {
        fixture.configureByText("test_at.cfg", "public net.minecraft.Minecraft <caret>")
        val lookupElements = fixture.completeBasic()
        val lookupStrings = lookupElements.map { it.lookupString }
        val expectedStrings =
            setOf("privString", "add(I)V", "copy(L;)V", "method()V", "overloaded()V", "overloaded(Ljava/lang/String;)V")
        assertEqualsUnordered(expectedStrings, lookupStrings)
    }

    @Test
    @DisplayName("Full Class Name Completion")
    fun fullClassNameCompletion() {
        doCompletionTest(
            "public <caret>",
            "public net.minecraft.Minecraft",
            "net.minecraft.Minecraft"
        )
        doCompletionTest(
            "public <caret>",
            "public net.minecraft.server.MinecraftServer",
            "net.minecraft.server.MinecraftServer"
        )
    }

    @Test
    @DisplayName("Field Name Completion")
    fun fieldNameCompletion() {
        doCompletionTest(
            "public net.minecraft.Minecraft privS<caret>",
            "public net.minecraft.Minecraft privString"
        )
    }

    @Test
    @DisplayName("Method Name Completion")
    fun methodNameCompletion() {
        doCompletionTest(
            "public net.minecraft.Minecraft add<caret>",
            "public net.minecraft.Minecraft add(I)V"
        )
    }

    @Test
    @DisplayName("Method Name Completion Cleaning End Of Line")
    fun methodNameCompletionCleaningEndOfLine() {
        doCompletionTest(
            "public net.minecraft.Minecraft overload<caret>ed(Ljava/some)V invalid; stuff",
            "public net.minecraft.Minecraft overloaded(Ljava/lang/String;)V",
            "overloaded(Ljava/lang/String;)V"
        )
    }
}
