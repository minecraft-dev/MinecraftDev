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
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.demonwav.mcdev.platform.mcp.McpModuleType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Transformer Usage Inspection Tests")
class AtUsageInspectionTest : BaseMinecraftTest(PlatformType.MCP, PlatformType.NEOFORGE) {

    @Test
    @DisplayName("Usage Inspection")
    fun usageInspection() {
        buildProject {
            java(
                "net/minecraft/Used.java",
                """
                package net.minecraft;
                public class Used {
                    public int usedField;
                    public int unusedField;
                    public void usedMethod() {}
                    public void unusedMethod() {}
                }
                """.trimIndent(),
                allowAst = true
            )
            java(
                "net/minecraft/server/Unused.java",
                """
                package net.minecraft.server;
                public class Unused {}
                """.trimIndent(),
                allowAst = true
            )
            java(
                "com/demonwav/mcdev/mcp/test/TestMod.java",
                """
                package com.demonwav.mcdev.mcp.test;
                public class TestMod {
                    public TestMod () {
                        net.minecraft.Used mc = new net.minecraft.Used();
                        int value = mc.usedField;
                        mc.usedMethod();
                    }
                }
                """.trimIndent(),
                allowAst = true
            )
            at(
                "test_at.cfg",
                """
                public net.minecraft.Used
                public net.minecraft.Used usedField
                <warning descr="Access Transformer entry is never used">public net.minecraft.Used unusedField</warning>
                public net.minecraft.Used usedMethod()V
                <warning descr="Access Transformer entry is never used">public net.minecraft.Used unusedMethod()V</warning>
                <warning descr="Access Transformer entry is never used">public net.minecraft.server.Unused</warning>
                """.trimIndent()
            )
        }

        // Force 1.20.2 because we test the non-SRG member names with NeoForge
        MinecraftFacet.getInstance(fixture.module, McpModuleType)!!
            .updateSettings(McpModuleSettings.State(minecraftVersion = "1.20.2"))

        fixture.enableInspections(AtUsageInspection::class.java)
        fixture.checkHighlighting()
    }
}
