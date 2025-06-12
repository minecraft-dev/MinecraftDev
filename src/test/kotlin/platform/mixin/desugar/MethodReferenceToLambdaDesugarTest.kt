/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.desugar

import com.demonwav.mcdev.platform.mixin.handlers.desugar.MethodReferenceToLambdaDesugarer
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes

@Suppress("Convert2MethodRef")
class MethodReferenceToLambdaDesugarTest : AbstractDesugarTest() {
    override val desugarer = MethodReferenceToLambdaDesugarer

    @Test
    fun testNoLambda() {
        doTestNoChange(
            """
                class Test {
                    Runnable r = Test::run;
                
                    private static void run() {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testNoLambdaThis() {
        doTestNoChange(
            """
                class Test {
                    Runnable r = this::run;
                    
                    private void run() {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testNoLambdaConstructor() {
        doTestNoChange(
            """
                import java.util.function.Supplier;

                class Test {
                    Supplier<Test> s = Test::new;
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaArray() {
        doTest(
            """
                import java.util.function.UnaryOperator;
                
                class Test {
                    UnaryOperator<Object[]> o = Object[]::clone;
                }
            """.trimIndent(),
            """
                import java.util.function.UnaryOperator;
                
                class Test {
                    UnaryOperator<Object[]> o = objects -> objects.clone();
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaSuper() {
        doTest(
            """
                import java.util.function.Supplier;
                
                class Test {
                    Supplier<Object> s = super::clone;
                }
            """.trimIndent(),
            """
                import java.util.function.Supplier;
                
                class Test {
                    Supplier<Object> s = () -> super.clone();
                }
            """.trimIndent()
        )
    }

    @Test
    fun testNoLambdaStaticInner() {
        doTestNoChange(
            """
                import java.util.function.Supplier;
                
                class Test {
                    Supplier<Inner> s = Inner::new;
                    
                    static class Inner {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaNonStaticInner() {
        doTest(
            """
                import java.util.function.Supplier;
                
                class Test {
                    Supplier<Inner> s = Inner::new;
                    
                    class Inner {
                    }
                }
            """.trimIndent(),
            """
                import java.util.function.Supplier;
                
                class Test {
                    Supplier<Inner> s = () -> new Inner();
                
                    class Inner {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaVarArgs() {
        doTest(
            """
                import java.util.function.BiFunction;
                
                class Test {
                    BiFunction<String, String, String> f = Test::func;
                    BiFunction<String, String, String> f2 = Test::func2;
                    
                    private static String func(String... args) {
                        return args[0];
                    }
                    
                    private static String func2(String arg1, String... args) {
                        return arg1;
                    }
                }
            """.trimIndent(),
            """
                import java.util.function.BiFunction;
                
                class Test {
                    BiFunction<String, String, String> f = (args, args2) -> func(args, args2);
                    BiFunction<String, String, String> f2 = (arg1, args) -> func2(arg1, args);
                
                    private static String func(String... args) {
                        return args[0];
                    }
                
                    private static String func2(String arg1, String... args) {
                        return arg1;
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testInstanceLambdaVarArgs() {
        doTest(
            """
                import java.util.function.BiFunction;                

                class Test {
                    BiFunction<Test, String, String> f = Test::func;
                    
                    private String func(String... args) {
                        return args[0];
                    }
                }
            """.trimIndent(),
            """
                import java.util.function.BiFunction;
                
                class Test {
                    BiFunction<Test, String, String> f = (test, args) -> test.func(args);
                
                    private String func(String... args) {
                        return args[0];
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaNoVarArgs() {
        doTestNoChange(
            """
                import java.util.function.Function;
                
                class Test {
                    Function<String[], String> f = Test::func;
                    
                    private static String func(String... args) {
                        return args[0];
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testInstanceLambdaNoVarArgs() {
        doTestNoChange(
            """
                import java.util.function.BiFunction;
                
                class Test {
                    BiFunction<Test, String[], String> f = Test::func;
                    
                    private String func(String... args) {
                        return args[0];
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testNoLambdaPrivateInner() {
        doTestNoChange(
            """
                class Test {
                    Runnable r = Inner::method;
                    
                    static class Inner {
                        private static void method() {
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaPrivateInnerJava8() {
        doTest(
            """
                class Test {
                    Runnable r = Inner::method;
                    
                    static class Inner {
                        private static void method() {
                        }
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    Runnable r = () -> Inner.method();
                
                    static class Inner {
                        private static void method() {
                        }
                    }
                }
            """.trimIndent(),
            classVersion = Opcodes.V1_8
        )
    }

    @Test
    fun testLambdaProtected() {
        doTest(
            """
                import java.io.ObjectOutputStream;
                import java.util.function.Consumer;
                
                class Test extends ObjectOutputStream {
                    Consumer<Object> c = this::writeObjectOverride;
                }
            """.trimIndent(),
            """
                import java.io.ObjectOutputStream;
                import java.util.function.Consumer;
                
                class Test extends ObjectOutputStream {
                    Consumer<Object> c = obj -> writeObjectOverride(obj);
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaIntersectionType() {
        doTest(
            """
                import java.io.Serializable;
                
                class Test {
                    F f = Test::method;
                    
                    private static <T extends Runnable & Serializable> void method(T t) {
                    }
                
                    @FunctionalInterface
                    interface F {
                        <T extends Runnable & Serializable> void accept(T t);
                    }
                }
            """.trimIndent(),
            """
                import java.io.Serializable;
                
                class Test {
                    F f = t -> {
                        method(t);
                    };
                
                    private static <T extends Runnable & Serializable> void method(T t) {
                    }
                
                    @FunctionalInterface
                    interface F {
                        <T extends Runnable & Serializable> void accept(T t);
                    }
                }
            """.trimIndent()
        )
    }
}
