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

package com.demonwav.mcdev.framework

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.facet.MinecraftFacetConfiguration
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import org.junit.jupiter.api.BeforeEach

abstract class BaseMinecraftTest(
    vararg platformTypes: PlatformType,
) : ProjectBuilderTest(
    getProjectDescriptor(platformTypes),
) {
    protected open val testPath = ""

    @BeforeEach
    fun setUp() {
        if (testPath.isNotBlank()) {
            fixture.testDataPath = "$BASE_DATA_PATH/$testPath"
        }
    }
}

fun getProjectDescriptor(platformTypes: Array<out PlatformType>): LightProjectDescriptor {
    return object : DefaultLightProjectDescriptor() {
        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel = LanguageLevel.JDK_1_8

            val facetManager = FacetManager.getInstance(module)
            val configuration = MinecraftFacetConfiguration()
            // The project auto detector will remove auto detect types we add here (since the actual libraries aren't present)
            // but we can set them manually as user set types and it will leave them alone
            platformTypes.forEach { configuration.state.userChosenTypes[it] = true }

            val facet = facetManager.createFacet(MinecraftFacet.facetType, "Minecraft", configuration, null)
            runWriteTask {
                val modifiableModel = facetManager.createModifiableModel()
                modifiableModel.addFacet(facet)
                modifiableModel.commit()
            }
        }

        override fun getSdk() = mockJdk
    }
}
