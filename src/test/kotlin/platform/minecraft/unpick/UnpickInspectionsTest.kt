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
import com.demonwav.mcdev.platform.minecraft.unpick.inspections.DuplicateUnpickConstantInspection
import com.demonwav.mcdev.platform.minecraft.unpick.inspections.InvalidUnpickConstantFieldInspection
import com.demonwav.mcdev.platform.minecraft.unpick.inspections.UnknownUnpickGroupInspection
import com.demonwav.mcdev.platform.minecraft.unpick.inspections.UnpickDivisionByZeroInspection
import com.demonwav.mcdev.platform.minecraft.unpick.inspections.UnpickExpressionTypeInspection
import com.demonwav.mcdev.platform.minecraft.unpick.inspections.UnpickGroupTypeConflictInspection
import com.demonwav.mcdev.platform.minecraft.unpick.inspections.UnpickTargetMismatchInspection
import com.demonwav.mcdev.platform.minecraft.unpick.inspections.UnresolvedUnpickReferenceInspection
import com.intellij.codeInspection.LocalInspectionTool
import kotlin.reflect.KClass
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Unpick Inspections Tests")
class UnpickInspectionsTest : BaseMinecraftTest() {

    @BeforeEach
    fun setupProject() {
        buildProject {
            java(
                "com/example/Constants.java",
                """
                package com.example;

                public class Constants {
                    public static final int ONE = 1;
                    public static final int TWO = 2;
                    public static final int ALSO_ONE = 1;
                    public static final long BIG = 10L;
                    public static final String NAME = "name";
                    public static int notFinal = 3;
                    public static final int NOT_CONSTANT = Integer.parseInt("4");
                    public final int instanceField = 5;
                    public static final boolean FLAG = true;

                    public Constants(int x) {
                    }

                    public static int method(int a, String b) {
                        return a;
                    }

                    public static void voidMethod(Object o) {
                    }

                    public static class Inner {
                        public static final int VALUE = 7;
                        public static final char CHAR = 'c';
                    }
                }
                """,
                configure = false,
                allowAst = true,
            )
            java(
                "com/example/Flags.java",
                """
                package com.example;

                public @interface Flags {
                }
                """,
                configure = false,
                allowAst = true,
            )
        }
    }

    private fun doTest(inspection: KClass<out LocalInspectionTool>, @Language("Unpick") code: String) {
        fixture.enableInspections(inspection)
        buildProject {
            unpick("test.unpick", code)
        }
        fixture.checkHighlighting(true, false, true)
    }

    @Test
    @DisplayName("Unresolved References")
    fun unresolvedReferences() = doTest(
        UnresolvedUnpickReferenceInspection::class,
        """
        unpick v4
        target_method com.example.Constants method (ILjava/lang/String;)I
        target_method com.example.Constants <init> (I)V
        target_method com.example.<error descr="Cannot resolve class 'com.example.Missing'">Missing</error> foo ()V
        target_method com.example.Constants <error descr="Cannot resolve method 'method' with descriptor '(I)I' in class 'com.example.Constants'">method</error> (I)I
        target_field com.example.Constants${'$'}Inner VALUE I g
        target_field com.example.Constants${'$'}<error descr="Cannot resolve class 'com.example.Constants${'$'}Missing'">Missing</error> VALUE I g
        target_field com.example.Constants <error descr="Cannot resolve field 'MISSING' in class 'com.example.Constants'">MISSING</error> I g
        target_annotation com.<error descr="Cannot resolve package 'com.nope'">nope</error>.Foo g
        group int g
            @scope package com.example
            @scope package com.example.<error descr="Cannot resolve package 'com.example.nope'">nope</error>
            @scope method com.example.Constants method (ILjava/lang/String;)I
            com.example.Constants.ONE
            com.example.Constants.<error descr="Cannot resolve field 'MISSING' in class 'com.example.Constants'">MISSING</error>
            com.example.Constants.*
        """,
    )

    @Test
    @DisplayName("Unknown Groups")
    fun unknownGroups() = doTest(
        UnknownUnpickGroupInspection::class,
        """
        unpick v4
        group int Known
            1
        target_annotation com.example.Flags Known
        target_annotation com.example.Flags <warning descr="Cannot find a definition of group 'Unknown'">Unknown</warning>
        """,
    )

    @Test
    @DisplayName("Group Type Conflicts")
    fun groupTypeConflicts() = doTest(
        UnpickGroupTypeConflictInspection::class,
        """
        unpick v4
        group <error descr="Group 'G' is also defined with data type long">int</error> G
            1
        group <error descr="Group 'G' is also defined with data type int">long</error> G
            2
        group int H
            1
        group int H
            2
        """,
    )

    @Test
    @DisplayName("Invalid Constant Fields")
    fun invalidConstantFields() = doTest(
        InvalidUnpickConstantFieldInspection::class,
        """
        unpick v4
        group int
            com.example.Constants.ONE
            com.example.Constants.<error descr="Field 'notFinal' is not final">notFinal</error>
            com.example.Constants.<error descr="Field 'NOT_CONSTANT' is not initialized to a compile-time constant">NOT_CONSTANT</error>
            com.example.Constants.<error descr="Field 'instanceField' is not static, add the ':instance' suffix">instanceField</error>
            com.example.Constants.instanceField:instance
            com.example.Constants.<error descr="Field 'ONE' is static, remove the ':instance' suffix">ONE</error>:instance
            com.example.Constants.ONE:<error descr="Field 'ONE' has type 'int', not 'long'">long</error>
            com.example.Constants.<error descr="Field 'FLAG' has type 'boolean', which cannot be used as a constant">FLAG</error>
        group String
            com.example.Constants.<error descr="No fields in 'com.example.Constants' match this wildcard">*</error>:instance
        """,
    )

    @Test
    @DisplayName("Expression Types")
    fun expressionTypes() = doTest(
        UnpickExpressionTypeInspection::class,
        """
        unpick v4
        group int
            com.example.Constants.ONE + 1
            'a'
            <error descr="Constant of type 'long' is not compatible with group type 'int'">com.example.Constants.BIG</error>
            <error descr="Constant of type 'String' is not compatible with group type 'int'">"str"</error>
            <error descr="Cannot cast 'String' to 'int'">(int) "str"</error>
            <error descr="Operator '<<' cannot be applied to 'float' and 'int'">1.0f << 1</error>
            <error descr="Operator '~' cannot be applied to 'double'">~1.0</error>
        group long
            com.example.Constants.BIG
            com.example.Constants.ONE
        group double
            1.5f * 2
        group String
            "a" + 1
            com.example.Constants.NAME
            <error descr="Operator '-' cannot be applied to 'String' and 'int'">"a" - 1</error>
        """,
    )

    @Test
    @DisplayName("Division By Zero")
    fun divisionByZero() = doTest(
        UnpickDivisionByZeroInspection::class,
        """
        unpick v4
        group int
            1 / <error descr="Division by zero">0</error>
            1 % <error descr="Division by zero">(com.example.Constants.ONE - 1)</error>
        group double
            1.0 / 0
        """,
    )

    @Test
    @DisplayName("Duplicate Constants")
    fun duplicateConstants() = doTest(
        DuplicateUnpickConstantInspection::class,
        """
        unpick v4
        group int G
            <warning descr="Constant evaluates to 1, which is also the value of 'com.example.Constants.ALSO_ONE'">com.example.Constants.ONE</warning>
            <warning descr="Constant evaluates to 1, which is also the value of 'com.example.Constants.ONE'">com.example.Constants.ALSO_ONE</warning>
            com.example.Constants.TWO
        group int G
            @scope class com.example.Constants
            com.example.Constants.TWO
        group long
            1
            1.0
        group long
            <warning descr="Constant evaluates to 2L, which is also the value of '1 + 1'">2</warning>
            <warning descr="Constant evaluates to 2L, which is also the value of '2'">1 + 1</warning>
        group int W
            <warning descr="Constant evaluates to 7 when the wildcard is expanded to 'Inner.VALUE', which is also the value of 'com.example.Constants${'$'}Inner.CHAR - 92'">com.example.Constants${'$'}Inner.*</warning>
            <warning descr="Constant evaluates to 7 when the wildcard is expanded to 'Inner.CHAR', which is also the value of 'com.example.Constants${'$'}Inner.VALUE'">com.example.Constants${'$'}Inner.* - 92</warning>
        """,
    )

    @Test
    @DisplayName("Target Mismatches")
    fun targetMismatches() = doTest(
        UnpickTargetMismatchInspection::class,
        """
        unpick v4
        group int I
            1
        group String S
            ""
        group long L
            1
        target_method com.example.Constants method (ILjava/lang/String;)I
            param 0 I
            param 1 S
            param <error descr="Parameter index 2 is out of bounds for a method with 2 parameters">2</error> I
            return L
        target_method com.example.Constants method (ILjava/lang/String;)I
            param 1 <error descr="Parameter 1 of type 'java.lang.String' is not compatible with group 'I' of type 'int'">I</error>
            return <error descr="Return type 'int' is not compatible with group 'S' of type 'String'">S</error>
        target_method com.example.Constants voidMethod (Ljava/lang/Object;)V
            param 0 <error descr="Parameter 0 of type 'java.lang.Object' is not compatible with group 'I' of type 'int'">I</error>
            <error descr="Method returns void">return</error> I
        target_field com.example.Constants${'$'}Inner CHAR C L
        target_field com.example.Constants${'$'}Inner VALUE <error descr="Field 'VALUE' has descriptor 'I'">J</error> L
        target_field com.example.Constants NAME Ljava/lang/String; <error descr="Field of type 'java.lang.String' is not compatible with group 'L' of type 'long'">L</error>
        target_annotation com.example.Flags I
        target_annotation <error descr="'com.example.Constants' is not an annotation type">com.example.Constants</error> I
        """,
    )
}
