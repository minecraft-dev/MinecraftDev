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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickExpressionEvaluator
import com.demonwav.mcdev.platform.minecraft.unpick.eval.UnpickValue
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupConstant
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.PsiTreeUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Unpick Expression Evaluator Tests")
class UnpickExpressionEvaluatorTest : BaseMinecraftTest() {

    @BeforeEach
    fun setupProject() {
        buildProject {
            java(
                "com/example/Wild.java",
                """
                package com.example;

                public class Wild {
                    public static final int A = 1;
                    public static final int B = 2;
                    public static final long C = 3;
                    public final int D = 4;
                    public static final String S = "s";
                    public static final byte SMALL = 5;
                    public static final Class<?> INT_TYPE = Integer.TYPE;
                    public static final Class<?> STRING_CLASS = String.class;
                    public static final String NULL_STRING = null;
                    public static final String ALIAS_NULL_STRING = NULL_STRING;
                    public static final Class<?> IFACE_CLASS = Iface.class;
                    public static final Class<?> ARRAY_CLASS = String[].class;
                    public static final Class<?> NOT_CONSTANT_CLASS = Wild.class.getClass();
                    public static int notFinal = 6;
                    public static final int NOT_CONSTANT = Integer.parseInt("7");
                }
                """,
                configure = false,
                allowAst = true,
            )
            java(
                "com/example/Iface.java",
                """
                package com.example;

                public interface Iface {
                }
                """,
                configure = false,
                allowAst = true,
            )
        }
    }

    private fun evaluateWithProblems(type: String, expression: String): UnpickExpressionEvaluator.Result {
        val file = fixture.configureByText("test.unpick", "unpick v4\ngroup $type\n    $expression\n")
        val constant = PsiTreeUtil.findChildOfType(file, UnpickGroupConstant::class.java)!!
        return UnpickExpressionEvaluator.evaluate(constant)
    }

    private fun evaluate(type: String, expression: String): List<Any?> {
        val result = evaluateWithProblems(type, expression)
        Assertions.assertEquals(emptyList<String>(), result.problems.map { it.message }, expression)
        return result.values.map { it.value.value }
    }

    private fun assertValue(expected: Any?, expression: String, type: String = "int") {
        Assertions.assertEquals(listOf(expected), evaluate(type, expression), expression)
    }

    @Test
    @DisplayName("Integer Arithmetic")
    fun integerArithmetic() {
        assertValue(7, "1 + 2 * 3")
        assertValue(3, "7 / 2")
        assertValue(-1, "-7 % 3")
        assertValue(Int.MIN_VALUE, "-2147483648")
        assertValue(Int.MIN_VALUE, "2147483647 + 1")
        assertValue(-1, "0xFFFFFFFF")
        assertValue(-1, "~0")
        assertValue(6, "0b101 ^ 0b011")
        assertValue(5, "0b101 & 7 | 0")
    }

    @Test
    @DisplayName("Shifts")
    fun shifts() {
        assertValue(2, "1 << 33")
        assertValue(8589934592L, "1L << 33", "long")
        assertValue(15, "-1 >>> 28")
        assertValue(-1, "-1 >> 28")
    }

    @Test
    @DisplayName("Promotion And Casts")
    fun promotionAndCasts() {
        assertValue(98, "'a' + 1")
        assertValue(3.5, "7.0 / 2", "double")
        assertValue(2.0f, "1.0f + 1", "float")
        assertValue(44.toByte(), "(byte) 300")
        assertValue(3, "(int) 3.9")
        assertValue(5.toByte(), "com.example.Wild.SMALL")
        assertValue(6, "com.example.Wild.SMALL + 1")
    }

    @Test
    @DisplayName("String Concatenation")
    fun stringConcatenation() {
        assertValue("a12", "\"a\" + 1 + 2", "String")
        assertValue("3a", "1 + 2 + \"a\"", "String")
        assertValue("A", "(char) 65 + \"\"", "String")
        assertValue("x1.0", "\"x\" + 1.0f", "String")
        assertValue("ss", "com.example.Wild.S + com.example.Wild.S", "String")
        assertValue(null, "com.example.Wild.NULL_STRING", "String")
        assertValue(null, "com.example.Wild.ALIAS_NULL_STRING", "String")
        assertValue("anull", "\"a\" + com.example.Wild.ALIAS_NULL_STRING", "String")
    }

    @Test
    @DisplayName("Class Concatenation")
    fun classConcatenation() {
        assertValue("int", "\"\" + com.example.Wild.INT_TYPE", "String")
        assertValue("class java.lang.String", "\"\" + com.example.Wild.STRING_CLASS", "String")
        assertValue("interface com.example.Iface", "\"\" + com.example.Wild.IFACE_CLASS", "String")
        assertValue("class [Ljava.lang.String;", "\"\" + com.example.Wild.ARRAY_CLASS", "String")
    }

    @Test
    @DisplayName("Class Constants")
    fun classConstants() {
        assertValue(PsiTypes.intType(), "com.example.Wild.INT_TYPE", "Class")
        Assertions.assertEquals(
            "java.lang.String",
            (evaluate("Class", "com.example.Wild.STRING_CLASS").single() as PsiType).canonicalText,
        )

        val result = evaluateWithProblems("Class", "com.example.Wild.NOT_CONSTANT_CLASS")
        Assertions.assertEquals(
            listOf("Field 'NOT_CONSTANT_CLASS' is not initialized to a compile-time constant"),
            result.problems.map { it.message },
        )
        Assertions.assertFalse(result.values.single().value.isKnown)
    }

    @Test
    @DisplayName("Wildcards")
    fun wildcards() {
        // Wildcards only match constant fields, and have the type of the group
        Assertions.assertEquals(listOf(1, 2, 5), evaluate("int", "com.example.Wild.*"))
        Assertions.assertEquals(listOf(1L, 2L, 3L, 5L), evaluate("long", "com.example.Wild.*"))
        Assertions.assertEquals(listOf(5.toByte()), evaluate("int", "com.example.Wild.*:byte"))
        Assertions.assertEquals(listOf("s", null, null), evaluate("String", "com.example.Wild.*"))
        Assertions.assertEquals(listOf(3L), evaluate("long", "com.example.Wild.*:long"))
        Assertions.assertEquals(listOf(4), evaluate("int", "com.example.Wild.*:instance"))
        Assertions.assertEquals(listOf(11, 12, 15), evaluate("int", "com.example.Wild.* + 10"))
    }

    @Test
    @DisplayName("Display Strings")
    fun displayStrings() {
        Assertions.assertEquals("\"a\\\"b\\n\\u0001\"", UnpickValue(DataType.STRING, "a\"b\n\u0001").toDisplayString())
        Assertions.assertEquals("'\\''", UnpickValue(DataType.CHAR, '\'').toDisplayString())
        Assertions.assertEquals("'\\t'", UnpickValue(DataType.CHAR, '\t').toDisplayString())
    }
}
