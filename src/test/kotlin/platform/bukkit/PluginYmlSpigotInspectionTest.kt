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

package com.demonwav.mcdev.platform.bukkit

import com.demonwav.mcdev.framework.BASE_DATA_PATH_2
import com.demonwav.mcdev.yaml.PluginYmlInspection
import com.intellij.testFramework.TestDataPath
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private const val TEST_DATA_PATH = "platform/bukkit/pluginYmlInspection"

@DisplayName("Plugin Yml Spigot Inspection Test")
@TestDataPath(BASE_DATA_PATH_2 + TEST_DATA_PATH)
class PluginYmlSpigotInspectionTest : BaseSpigotTest() {

    override val testPath: String = TEST_DATA_PATH

    private fun doTest(@Language("yaml") pluginYml: String) {
        fixture.configureByFile("SimplePlugin.java")
        fixture.configureByText("plugin.yml", pluginYml)
        fixture.enableInspections(PluginYmlInspection::class)
        fixture.checkHighlighting()
    }

    @Test
    @DisplayName("Main Unresolved Reference")
    fun mainUnresolvedReference() {
        doTest("main: <error descr=\"Unresolved reference\">test.BadReference</error>")
    }

    @Test
    @DisplayName("Main Wrong Type")
    fun mainWrongType() {
        doTest("main: <error descr=\"Class must implement org.bukkit.plugin.Plugin\">java.lang.String</error>")
    }

    @Test
    @DisplayName("Main Good Type")
    fun mainGoodType() {
        doTest("main: test.SimplePlugin")
    }
}
