package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPackage
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Transformer References Tests")
class AtReferencesTest : BaseMinecraftTest(PlatformType.MCP, PlatformType.NEOFORGE) {

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

        // Force 1.20.2 because we test the non-SRG member names with NeoForge
        MinecraftFacet.getInstance(fixture.module, McpModuleType)!!
            .updateSettings(McpModuleSettings.State(minecraftVersion = "1.20.2"))
    }

    private inline fun <reified E : PsiElement> testReferenceAtCaret(
        @Language("Access Transformers") at: String,
        crossinline test: (element: E) -> Unit
    ) {
        fixture.configureByText("test_at.cfg", at)
        runReadAction {
            val ref = fixture.getReferenceAtCaretPositionWithAssertion()
            val resolved = ref.resolve().also(::assertNotNull)!!
            test(assertInstanceOf(E::class.java, resolved))
        }
    }

    @Test
    @DisplayName("Package Reference")
    fun packageReference() {
        testReferenceAtCaret<PsiPackage>("public com.demonwav.mcdev.<caret>mcp.test.TestLibrary privString") { pack ->
            val expectedPackage = fixture.findPackage("com.demonwav.mcdev.mcp")
            assertEquals(expectedPackage, pack)
        }
    }

    @Test
    @DisplayName("Class Reference")
    fun classReference() {
        testReferenceAtCaret<PsiClass>("public com.demonwav.mcdev.mcp.test.<caret>TestLibrary privString") { clazz ->
            val expectedClass = fixture.findClass("com.demonwav.mcdev.mcp.test.TestLibrary")
            assertEquals(expectedClass, clazz)
        }
    }

    @Test
    @DisplayName("Field Reference")
    fun fieldReference() {
        testReferenceAtCaret<PsiField>("public com.demonwav.mcdev.mcp.test.TestLibrary <caret>privString") { field ->
            val expectedClass = fixture.findClass("com.demonwav.mcdev.mcp.test.TestLibrary")
            val expectedField = expectedClass.findFieldByName("privString", false)
            assertEquals(expectedField, field)
        }
    }

    @Test
    @DisplayName("Method Reference")
    fun methodReference() {
        testReferenceAtCaret<PsiMethod>("public com.demonwav.mcdev.mcp.test.TestLibrary <caret>method()V") { method ->
            val expectedClass = fixture.findClass("com.demonwav.mcdev.mcp.test.TestLibrary")
            val expectedMethod = expectedClass.findMethodsByName("method", false).single()
            assertEquals(expectedMethod, method)
        }
    }

    @Test
    @DisplayName("Method Overload Reference")
    fun methodOverloadReference() {
        testReferenceAtCaret<PsiMethod>(
            "public com.demonwav.mcdev.mcp.test.TestLibrary <caret>overloaded()V"
        ) { method ->
            val expectedClass = fixture.findClass("com.demonwav.mcdev.mcp.test.TestLibrary")
            val expectedMethod = expectedClass.findMethodsByName("overloaded", false).single { !it.hasParameters() }
            assertEquals(expectedMethod, method)
        }

        testReferenceAtCaret<PsiMethod>(
            "public com.demonwav.mcdev.mcp.test.TestLibrary <caret>overloaded(Ljava/lang/String;)V"
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
            "public com.demonwav.mcdev.mcp.test.TestLibrary copy(Ljava/lang/<caret>String;)V"
        ) { clazz ->
            val expectedClass = fixture.findClass(CommonClassNames.JAVA_LANG_STRING)
            assertEquals(expectedClass, clazz)
        }
    }

    @Test
    @DisplayName("Descriptor Primitive Type Reference")
    fun descriptorPrimitiveTypeReference() {
        testReferenceAtCaret<PsiClass>(
            "public com.demonwav.mcdev.mcp.test.TestLibrary copy(<caret>I)V"
        ) { clazz ->
            val expectedClass = fixture.findClass(CommonClassNames.JAVA_LANG_INTEGER)
            assertEquals(expectedClass, clazz)
        }
    }
}
