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
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.modules
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.platform.externalSystem.testFramework.ExternalSystemImportingTestCase
import java.nio.file.FileSystemException
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/a1c5c737ba52cd5810c95d28e6e685d11958844b/plugins/gradle/testSources/org/jetbrains/plugins/gradle/importing/GradleImportingTestCase.java
 */
abstract class BaseGradleImportTest : ExternalSystemImportingTestCase() {

    companion object {
        const val GRADLE_WRAPPER_PROPERTIES = "gradle/wrapper/gradle-wrapper.properties"
        const val BUILD_GRADLE = "build.gradle"
        const val BUILD_GRADLE_KT = "build.gradle.kts"
        const val GRADLE_PROPERTIES = "build.gradle"
        const val SETTINGS_GRADLE = "settings.gradle"
    }

    private val projectSettings = GradleProjectSettings().withQualifiedModuleNames()

    override fun getCurrentExternalProjectSettings(): ExternalProjectSettings? = projectSettings

    override fun getExternalSystemId(): ProjectSystemId? = GradleConstants.SYSTEM_ID

    override fun getTestsTempDir(): String? = "tmp"

    override fun getExternalSystemConfigFileName(): String? = "build.gradle"

    @BeforeEach
    @Suppress("JUnitMalformedDeclaration") // Hey JetBrains, it's working fine...
    fun setUp(testInfo: TestInfo) {
        name = testInfo.displayName
        super.setUp()

        runWriteTask {
            val jdk21Home = System.getenv("JDK_21_0") ?: System.getenv("JDK_21") ?: System.getenv("JDK21")
            assertNotNull("Could not find JDK 21 home", jdk21Home)

            val jdk = JavaSdk.getInstance().createJdk("JDK 21", jdk21Home, false)
            ProjectJdkTable.getInstance().addJdk(jdk)
            JavaSdkUtil.applyJdkToProject(myProject, jdk)
        }
    }

    @AfterEach
    override fun tearDown() {
        runWriteTask {
            val sdk = ProjectRootManager.getInstance(myProject).projectSdk
            ProjectRootManager.getInstance(myProject).projectSdk = null
            if (sdk != null) {
                ProjectJdkTable.getInstance().removeJdk(sdk)
            }
        }

        try {
            super.tearDown()
        } catch (e: FileSystemException) {
            // This is a frequent issue on Windows, but is harmless, except the fact it leaves the culprit file existing
            println("Ignoring FSException: ${e.message}")
        }
    }

    protected fun findModule(name: String): Module {
        val module = myProject.modules.find { it.name.contains(name) }
        assertNotNull("Could not find module containing '$name'", module)
        return module!!
    }

    protected fun findMainModule() = findModule("main")

    protected fun Module.assertDetectedPlatformTypes(vararg platformTypes: PlatformType) {
        val facet = MinecraftFacet.getInstance(this)
        assertNotNull("MinecraftFacet is null in module ${this.name}", facet)

        assertSameElements(facet!!.configuration.state.autoDetectTypes, platformTypes.toSet())
    }

    protected fun <T : AbstractModule> Module.assertMinecraftModule(moduleType: AbstractModuleType<T>): T {
        val facet = MinecraftFacet.getInstance(this)
        assertNotNull("MinecraftFacet is null in module ${this.name}", facet)

        val module = facet!!.getModuleOfType(moduleType)
        assertNotNull("Could not find Minecraft module of type ${moduleType.javaClass.name}", module)

        return module!!
    }
}
