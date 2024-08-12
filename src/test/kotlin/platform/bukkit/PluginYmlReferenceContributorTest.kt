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
import com.demonwav.mcdev.framework.EdtInterceptor
import com.intellij.psi.PsiClass
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.requireIs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private const val TEST_DATA_PATH = "platform/bukkit/pluginYmlReferenceContributor"

@ExtendWith(EdtInterceptor::class)
@DisplayName("Plugin Yml Reference Contributor Test")
@TestDataPath(BASE_DATA_PATH_2 + TEST_DATA_PATH)
class PluginYmlReferenceContributorTest : BaseSpigotTest() {

    override val testPath: String = TEST_DATA_PATH

    fun setupProject() {
        fixture.configureByFiles("SimplePlugin.java", "plugin.yml")
    }

    @Test
    @DisplayName("Main Class Reference")
    fun mainClassReference() {
        setupProject()
        val caretRef = fixture.getReferenceAtCaretPositionWithAssertion("plugin.yml")
        val resolvedClass = caretRef.resolve().requireIs<PsiClass>()
        Assertions.assertEquals("simple.plugin.SimplePlugin", resolvedClass.qualifiedName)
    }

    @Test
    @DisplayName("Main Class Reference Variants")
    fun mainClassReferenceVariants() {
        setupProject()
        fixture.testCompletionVariants("plugin.yml", "simple.plugin.SimplePlugin")
    }

    @Test
    @DisplayName("Main Class Reference Rename From Plugin Yml")
    fun mainClassReferenceRenameFromPluginYml() {
        fixture.configureByFiles("plugin.yml", "SimplePlugin.java") // The different order matters

        fixture.renameElementAtCaret("RenamedPlugin")
        fixture.checkResultByFile("plugin.yml", "afterRename/plugin.yml", false)
        fixture.checkResultByFile("RenamedPlugin.java", "afterRename/RenamedPlugin.java", false)
    }

    @Test
    @DisplayName("Main Class Reference Rename From Class")
    fun mainClassReferenceRenameFromClass() {
        setupProject()

        fixture.renameElementAtCaret("RenamedPlugin")
        fixture.checkResultByFile("plugin.yml", "afterRename/plugin.yml", false)
        fixture.checkResultByFile("RenamedPlugin.java", "afterRename/RenamedPlugin.java", false)
    }

    @Test
    @DisplayName("Main Class Reference Move")
    fun mainClassReferenceMove() {
        setupProject()

        fixture.tempDirFixture.findOrCreateDir("pack")
        fixture.moveFile("SimplePlugin.java", "pack")
        fixture.checkResultByFile("plugin.yml", "afterMove/plugin.yml", false)
    }
}
