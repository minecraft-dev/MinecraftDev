/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.MinecraftProjectSettings
import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.BaseMixinTest
import com.demonwav.mcdev.platform.mixin.util.MissingLVTChecker
import com.demonwav.mcdev.util.BeforeOrAfter
import com.demonwav.mcdev.util.McdevReflection.invokeUnwrap
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("MixinExtras expression completion test")
class MEExpressionCompletionTest : BaseMixinTest() {
    private fun assertLookupAppears(
        lookupString: String,
        @Language("JAVA") code: String,
        shouldAppear: Boolean = true
    ) {
        buildProject {
            dir("test") {
                java("MEExpressionCompletionTest.java", code)
            }
        }

        fixture.completeBasic()

        val lookups = fixture.lookupElementStrings
        if (lookups != null) {
            if (shouldAppear) {
                assertTrue(lookupString in lookups)
            } else {
                assertFalse(lookupString in lookups)
            }
        } else {
            if (shouldAppear) {
                assertEquals(lookupString, fixture.elementAtCaret.text)
            } else {
                assertNotEquals(lookupString, fixture.elementAtCaret.text)
            }
        }
    }

    private fun doBeforeAfterTest(
        lookupString: String,
        @Language("JAVA") code: String,
        @Language("JAVA") expectedAfter: String?
    ) {
        buildProject {
            dir("test") {
                java("MEExpressionCompletionTest.java", code)
            }
        }

        MinecraftProjectSettings.getInstance(fixture.project).definitionPosRelativeToExpression = BeforeOrAfter.BEFORE

        try {
            MEExpressionCompletionUtil.debugCompletionUnitTest = true
            val possibleItems = fixture.completeBasic()
            if (possibleItems != null) {
                val itemToComplete = possibleItems.firstOrNull { it.lookupString == lookupString }
                if (expectedAfter != null) {
                    assertNotNull(itemToComplete, "Expected a completion matching \"$lookupString\"")
                    fixture.lookup.currentItem = itemToComplete
                    fixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
                } else {
                    assertNull(itemToComplete, "Expected no completions matching \"$lookupString\"")
                    return
                }
            } else if (expectedAfter == null) {
                fail<Unit>("Expected no completions matching \"$lookupString\"")
                return
            }
        } finally {
            MEExpressionCompletionUtil.debugCompletionUnitTest = false
        }

        // TODO: this is for debugging, remove this once we figure out why tests are failing
        try {
            val getHostFile = CodeInsightTestFixtureImpl::class.java.getDeclaredMethod("getHostFile")
                .also { it.isAccessible = true }
            val actual = StringUtil.convertLineSeparators((getHostFile.invokeUnwrap(fixture) as PsiFile).text)
            assertEquals(
                expectedAfter.replace("<caret>", ""),
                actual
            ) {
                "File comparison failed, actual text: ${actual.ifEmpty { "<empty file oh no!>" }}"
            }
        } catch (e: ReflectiveOperationException) {
            System.err.println("Unable to call getHostFile")
        }

        fixture.checkResult(expectedAfter)
    }

    @Test
    @DisplayName("Local Variable Implicit Completion Test (No LVT)")
    fun localVariableImplicitCompletionNoLVTTest() {
        ExtensionTestUtil.addExtensions(MissingLVTChecker.EP_NAME, listOf(AlwaysMissingLVTChecker), fixture.testRootDisposable)
        doBeforeAfterTest(
            "one",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import com.llamalad7.mixinextras.sugar.Local;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "one", local = @Local(type = int.class))
                @Expression("one")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Local Variable Implicit Completion Test")
    fun localVariableImplicitCompletionTest() {
        doBeforeAfterTest(
            "one",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import com.llamalad7.mixinextras.sugar.Local;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "one", local = @Local(type = int.class, name = "one"))
                @Expression("one")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Local Variable Ordinal Completion Test (No LVT)")
    fun localVariableOrdinalCompletionNoLVTTest() {
        ExtensionTestUtil.addExtensions(MissingLVTChecker.EP_NAME, listOf(AlwaysMissingLVTChecker), fixture.testRootDisposable)
        doBeforeAfterTest(
            "local1",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import com.llamalad7.mixinextras.sugar.Local;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "local1", local = @Local(type = String.class, ordinal = 0))
                @Expression("local1")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Local Variable Ordinal Completion Test")
    fun localVariableOrdinalCompletionTest() {
        doBeforeAfterTest(
            "local1",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import com.llamalad7.mixinextras.sugar.Local;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "local1", local = @Local(type = String.class, name = "local1"))
                @Expression("local1")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Local Variable Inaccessible Type Completion Test (No LVT)")
    fun localVariableInaccessibleTypeCompletionNoLVTTest() {
        ExtensionTestUtil.addExtensions(MissingLVTChecker.EP_NAME, listOf(AlwaysMissingLVTChecker), fixture.testRootDisposable)
        doBeforeAfterTest(
            "varOfInaccessibleType",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "acceptInaccessibleType", method = "Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData;acceptInaccessibleType(Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData${'$'}InaccessibleType;)V")
                @Expression("acceptInaccessibleType(<caret>)")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import com.llamalad7.mixinextras.sugar.Local;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "acceptInaccessibleType", method = "Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData;acceptInaccessibleType(Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData${'$'}InaccessibleType;)V")
                @Definition(id = "varOfInaccessibleType", local = @Local(ordinal = 0))
                @Expression("acceptInaccessibleType(varOfInaccessibleType)")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Local Variable Inaccessible Type Completion Test")
    fun localVariableInaccessibleTypeCompletionTest() {
        doBeforeAfterTest(
            "varOfInaccessibleType",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "acceptInaccessibleType", method = "Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData;acceptInaccessibleType(Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData${'$'}InaccessibleType;)V")
                @Expression("acceptInaccessibleType(<caret>)")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import com.llamalad7.mixinextras.sugar.Local;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "acceptInaccessibleType", method = "Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData;acceptInaccessibleType(Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData${'$'}InaccessibleType;)V")
                @Definition(id = "varOfInaccessibleType", local = @Local(name = "varOfInaccessibleType"))
                @Expression("acceptInaccessibleType(varOfInaccessibleType)")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Field Completion Test")
    fun fieldCompletionTest() {
        doBeforeAfterTest(
            "out",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "out", field = "Ljava/lang/System;out:Ljava/io/PrintStream;")
                @Expression("out")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Method Completion Test")
    fun methodCompletionTest() {
        doBeforeAfterTest(
            "acceptInaccessibleType",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "acceptInaccessibleType", method = "Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData;acceptInaccessibleType(Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData${'$'}InaccessibleType;)V")
                @Expression("acceptInaccessibleType(<caret>)")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Method No-Arg Completion Test")
    fun methodNoArgCompletionTest() {
        doBeforeAfterTest(
            "noArgMethod",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "noArgMethod", method = "Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData;noArgMethod()V")
                @Expression("noArgMethod()<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Type Completion Test")
    fun typeCompletionTest() {
        doBeforeAfterTest(
            "ArrayList",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("new <caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            import java.util.ArrayList;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "ArrayList", type = ArrayList.class)
                @Expression("new ArrayList(<caret>)")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("Inaccessible Type Completion Test")
    fun inaccessibleTypeCompletionTest() {
        doBeforeAfterTest(
            "InaccessibleType",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("new <caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            null,
        )
    }

    @Test
    @DisplayName("Array Creation Completion Test")
    fun arrayCreationCompletionTest() {
        doBeforeAfterTest(
            "String",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("new <caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent(),
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "String", type = String.class)
                @Expression("new String[<caret>]")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("LHS Of Complete Assignment Test")
    fun lhsOfCompleteAssignmentTest() {
        assertLookupAppears(
            "local1",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("<caret> = 'Hello'")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Cast Test")
    fun castTest() {
        assertLookupAppears(
            "Integer",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("(<caret>)")
                @Inject(method = "getStingerCount", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Member Function Test")
    fun memberFunctionTest() {
        assertLookupAppears(
            "get",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "Integer", type = Integer.class)
                @Definition(id = "synchedData", field = "Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData;synchedData:Lcom/demonwav/mcdev/mixintestdata/meExpression/MEExpressionTestData${'$'}SynchedDataManager;")
                @Expression("(Integer) this.synchedData.<caret>")
                @Inject(method = "getStingerCount", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Array Length Test")
    fun arrayLengthTest() {
        assertLookupAppears(
            "one",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "String", type = String.class)
                @Expression("new String[<caret>]")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Array Element Test")
    fun arrayElementTest() {
        assertLookupAppears(
            "local2",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Definition;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Definition(id = "String", type = String.class)
                @Expression("new String[]{?, <caret>}")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Static Method Reference Test")
    fun staticMethodReferenceTest() {
        assertLookupAppears(
            "staticMapper",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("::<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Non Static Method Reference Test")
    fun nonStaticMethodReferenceTest() {
        assertLookupAppears(
            "nonStaticMapper",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("this::<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Constructor Method Reference Test")
    fun constructorMethodReferenceTest() {
        assertLookupAppears(
            "ConstructedByMethodReference",
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.meExpression.MEExpressionTestData;
            import com.llamalad7.mixinextras.expression.Expression;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            
            @Mixin(MEExpressionTestData.class)
            class MEExpressionCompletionTest {
                @Expression("<caret>")
                @Inject(method = "complexFunction", at = @At("MIXINEXTRAS:EXPRESSION"))
            }
            """.trimIndent()
        )
    }

    private object AlwaysMissingLVTChecker : MissingLVTChecker {
        override fun hasMissingLVT(context: PsiElement, className: String) = true
    }
}
