package com.demonwav.mcdev.platform.neoforge

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.neoforge.version.platform.neoforge.version.NeoModDevVersion
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.JavaTestUtil
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.project.modules
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.platform.externalSystem.testFramework.ExternalSystemImportingTestCase
import java.nio.file.FileSystemException
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInfo
import org.junitpioneer.jupiter.cartesian.ArgumentSets
import org.junitpioneer.jupiter.cartesian.CartesianTest

@DisplayName("NeoForge Import Tests")
class NeoForgeImportTest : ExternalSystemImportingTestCase() {

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
            val jdk = JavaTestUtil.setupInternalJdkAsTestJDK(testRootDisposable, "JDK")
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

    private fun prepareTest(neoModDevVersion: String, neoForgeVersion: String) {
        createProjectSubFile(
            "gradle/wrapper/gradle-wrapper.properties",
            "distributionUrl=https://services.gradle.org/distributions/gradle-8.8-bin.zip"
        )

        createProjectSubFile(
            "build.gradle",
            """
                plugins {
                    id 'net.neoforged.moddev' version '$neoModDevVersion'
                }
    
                neoForge {
                    version = "$neoForgeVersion"
                }
            """.trimIndent()
        )

        createProjectSubFile(
            "settings.gradle",
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        maven { url = 'https://maven.neoforged.net/releases' }
                    }
                }
            """
        )

        importProject(true)
        importProject(true) // Import twice otherwise McpModule settings won't be populated properly
    }

    @CartesianTest
    @CartesianTest.MethodFactory("versionFactory")
    @DisplayName("NeoForge Imported Data")
    fun neoForgeImportedData(
        neoModDevVersion: String,
        neoForgeAndExpectedMcVersion: Pair<String, String>
    ) {
        val (neoForgeVersion, expectedMcVersion) = neoForgeAndExpectedMcVersion
        prepareTest(neoModDevVersion, neoForgeVersion)

        val module = myProject.modules.find { it.name.contains("main") }
        assertNotNull(module)

        val facet = MinecraftFacet.getInstance(module!!)
        assertSameElements(facet!!.types, NeoForgeModuleType, McpModuleType, MixinModuleType)

        val mcpModule = MinecraftFacet.getInstance(module, McpModuleType)
        assertNotNull(mcpModule)

        val mcpSettings = mcpModule!!.getSettings()
        assertEquals(mcpSettings.platformVersion, neoForgeVersion)
        assertEquals(mcpSettings.minecraftVersion, expectedMcVersion)
    }

    companion object {
        @JvmStatic
        @Suppress("unused") // Used in @CartesianTest.MethodFactory
        fun versionFactory(): ArgumentSets? {
            val neoModDevVersions = mutableListOf("2.0.7-beta", "1.0.17", "0.1.131")

            val latestNeoModDev = runBlocking { NeoModDevVersion.downloadData()?.versions?.firstOrNull() }
            if (latestNeoModDev != null) {
                neoModDevVersions.add(0, latestNeoModDev.toString())
            }

            return ArgumentSets
                .argumentsForFirstParameter(neoModDevVersions)
                .argumentsForNextParameter(
                    "21.1.8" to "1.21.1",
                    "21.0.167" to "1.21",
                )
        }
    }
}
