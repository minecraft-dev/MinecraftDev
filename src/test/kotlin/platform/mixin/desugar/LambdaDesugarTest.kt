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

import com.demonwav.mcdev.platform.mixin.handlers.desugar.DesugarUtil
import com.demonwav.mcdev.platform.mixin.handlers.desugar.LambdaDesugarer
import java.lang.invoke.LambdaMetafactory
import org.junit.jupiter.api.Test
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

@Suppress(
    "Convert2MethodRef",
    "ObviousNullCheck",
    "rawtypes",
    "RedundantCast",
    "ResultOfMethodCallIgnored",
    "SwitchStatementWithTooFewBranches",
    "unchecked",
    "UnnecessarySemicolon"
)
class LambdaDesugarTest : AbstractDesugarTest() {
    override val desugarer = LambdaDesugarer

    companion object {
        internal val metafactory = Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false
        )
        private val altMetafactory = Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "altMetafactory",
            "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
            false
        )
    }

    @Test
    fun testSimpleMethodReference() {
        doIndyTest(
            """
                class Test {
                    void test() {
                        Runnable r = Test::method;
                    }
                
                    static void method() {
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    private static Runnable synthetic1() {
                    }
                    
                    void test() {
                        Runnable r = Test.synthetic1<caret>();
                    }
                
                    static void method() {
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "run",
                "()Ljava/lang/Runnable;",
                metafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "method",
                    "()V",
                    false
                ),
                Type.getMethodType("()V")
            )
        )
    }

    @Test
    fun testBoundMethodReference() {
        doIndyTest(
            """
                class Test {
                    void test() {
                        Runnable r = this::method;
                    }
                    
                    void method() {
                    }
                }
            """.trimIndent(),
            """
                import java.util.Objects;
                
                class Test {
                    private static Runnable synthetic1(Test param1) {
                    }
                    
                    void test() {
                        Runnable r = Test.synthetic1<caret>(Objects.requireNonNull(this));
                    }
                
                    void method() {
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "run",
                "(LTest;)Ljava/lang/Runnable;",
                metafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "Test",
                    "method",
                    "()V",
                    false
                ),
                Type.getMethodType("()V"),
            )
        )
    }

    @Test
    fun testConstructorMethodReference() {
        doIndyTest(
            """
                import java.util.function.Supplier;

                class Test {
                    void test() {
                        Supplier<Test> s = Test::new;
                    }
                }
            """.trimIndent(),
            """
                import java.util.function.Supplier;
                
                class Test {
                    private static Supplier synthetic1() {
                    }
                
                    void test() {
                        Supplier<Test> s = Test.synthetic1<caret>();
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "get",
                "()Ljava/util/function/Supplier;",
                metafactory,
                Type.getMethodType("()Ljava/lang/Object;"),
                Handle(
                    Opcodes.H_NEWINVOKESPECIAL,
                    "Test",
                    "<init>",
                    "()V",
                    false
                ),
                Type.getMethodType("()LTest;"),
            )
        )
    }

    @Test
    fun testSimpleLambda() {
        doIndyTest(
            """
                class Test {
                    void test() {
                        Runnable r = () -> {
                        };
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    private static void lambda${'$'}test$0() {
                    }
                
                    private static Runnable synthetic1() {
                    }
                
                    void test() {
                        Runnable r = Test.synthetic1<caret>();
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "run",
                "()Ljava/lang/Runnable;",
                metafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "lambda\$test$0",
                    "()V",
                    false
                ),
                Type.getMethodType("()V"),
            )
        )
    }

    @Test
    fun testNonStaticLambda() {
        doIndyTest(
            """
                class Test {
                    void test() {
                        Runnable r = () -> method();
                    }
                    
                    void method() {
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    private static Runnable synthetic1(Test param1) {
                    }
                
                    void test() {
                        Runnable r = Test.synthetic1<caret>(this);
                    }
                
                    void method() {
                    }
                
                    private void lambda${'$'}test$0() {
                        method();
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "run",
                "(LTest;)Ljava/lang/Runnable;",
                metafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "Test",
                    "lambda\$test$0",
                    "()V",
                    false
                ),
                Type.getMethodType("()V"),
            )
        )
    }

    @Test
    fun testLambdaInConstructor() {
        doTest(
            """
                class Test {
                    Test() {
                        Runnable r = () -> {
                        };
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    Test() {
                        Runnable r = Test.synthetic1();
                    }
                
                    private static void lambda${'$'}new$0() {
                    }
                
                    private static Runnable synthetic1() {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaInStaticInitializer() {
        doTest(
            """
                class Test {
                    static {
                        Runnable r = () -> {
                        };
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    static {
                        Runnable r = Test.synthetic1();
                    }
                
                    private static void lambda${'$'}static$0() {
                    }
                
                    private static Runnable synthetic1() {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaWithCapture() {
        doIndyTest(
            """
                class Test {
                    void test(String capture) {
                        Runnable r = () -> System.out.println(capture);
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    private static void lambda${'$'}test$0(String capture) {
                        System.out.println(capture);
                    }
                
                    private static Runnable synthetic1(String param1) {
                    }
                
                    void test(String capture) {
                        Runnable r = Test.synthetic1<caret>(capture);
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "run",
                "(Ljava/lang/String;)Ljava/lang/Runnable;",
                metafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "lambda\$test$0",
                    "(Ljava/lang/String;)V",
                    false
                ),
                Type.getMethodType("()V"),
            )
        )
    }

    @Test
    fun testNonStaticLambdaWithCapture() {
        doIndyTest(
            """
                class Test {
                    void test(String capture) {
                        Runnable r = () -> method(capture);
                    }
                    
                    void method(String s) {
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    private static Runnable synthetic1(Test param1, String param2) {
                    }
                
                    void test(String capture) {
                        Runnable r = Test.synthetic1<caret>(this, capture);
                    }
                
                    void method(String s) {
                    }
                
                    private void lambda${'$'}test$0(String capture) {
                        method(capture);
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "run",
                "(LTest;Ljava/lang/String;)Ljava/lang/Runnable;",
                metafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "Test",
                    "lambda\$test$0",
                    "(Ljava/lang/String;)V",
                    false
                ),
                Type.getMethodType("()V"),
            )
        )
    }

    @Test
    fun testLambdaMarkerInterface() {
        doIndyTest(
            """
                class Test {
                    void test() {
                        var r = (Runnable & Marker) () -> {
                        };
                    }
                    
                    interface Marker {
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    private static void lambda${'$'}test$0() {
                    }
                
                    private static Runnable synthetic1() {
                    }
                
                    void test() {
                        var r = (Runnable & Marker) Test.synthetic1<caret>();
                    }
                
                    interface Marker {
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "run",
                "()Ljava/lang/Runnable;",
                altMetafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "lambda\$test$0",
                    "()V",
                    false
                ),
                Type.getMethodType("()V"),
                LambdaMetafactory.FLAG_MARKERS or LambdaMetafactory.FLAG_BRIDGES,
                1,
                Type.getType("LTest\$Marker;"),
                0,
            )
        )
    }

    @Test
    fun testSerializableLambda() {
        doIndyTest(
            """
                import java.io.Serializable;

                class Test {
                    void test() {
                        var r = (Runnable & Serializable) () -> {
                        };
                    }
                }
            """.trimIndent(),
            """
                import java.io.Serializable;
                import java.lang.invoke.SerializedLambda;
                
                class Test {
                    private static void lambda${'$'}test$2feeadd5$1() {
                    }
                
                    private static Runnable synthetic1() {
                    }
                
                    private static Object ${'$'}deserializeLambda$(SerializedLambda serializedLambda) {
                        switch (serializedLambda.getImplMethodName()) {
                            case "lambda${'$'}test$2feeadd5$1":
                                if (serializedLambda.getImplMethodKind() == 6 && serializedLambda.getFunctionalInterfaceClass().equals("java/lang/Runnable") && serializedLambda.getFunctionalInterfaceMethodName().equals("run") && serializedLambda.getFunctionalInterfaceMethodSignature().equals("()V") && serializedLambda.getImplClass().equals("Test") && serializedLambda.getImplMethodSignature().equals("()V"))
                                    synthetic1();
                                break;
                        }
                        throw new IllegalArgumentException("Invalid lambda deserialization");
                    }
                
                    void test() {
                        var r = (Runnable & Serializable) Test.synthetic1<caret>();
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "run",
                "()Ljava/lang/Runnable;",
                altMetafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "lambda\$test$2feeadd5$1",
                    "()V",
                    false
                ),
                Type.getMethodType("()V"),
                LambdaMetafactory.FLAG_SERIALIZABLE or LambdaMetafactory.FLAG_BRIDGES,
                0,
            )
        )
    }

    @Test
    fun testMultipleSerializableMethodReferences() {
        doIndyTest(
            """
                import java.io.Serializable;

                class Test {
                    I test1() {
                        return (I & Serializable) Test::method;
                    }
                    
                    J test2() {
                        return (J & Serializable) Test::method;
                    }
                    
                    private static void method() {
                    }
                
                    @FunctionalInterface
                    interface I {
                        void foo();
                    }
                    
                    @FunctionalInterface
                    interface J {
                        void foo();
                    }
                }
            """.trimIndent(),
            """
                import java.io.Serializable;
                import java.lang.invoke.SerializedLambda;
                
                class Test {
                    private static J synthetic1() {
                    }
                
                    private static I synthetic2() {
                    }
                
                    private static Object ${'$'}deserializeLambda$(SerializedLambda serializedLambda) {
                        switch (serializedLambda.getImplMethodName()) {
                            case "method":
                                if (serializedLambda.getImplMethodKind() == 6 && serializedLambda.getFunctionalInterfaceClass().equals("Test${'$'}I") && serializedLambda.getFunctionalInterfaceMethodName().equals("foo") && serializedLambda.getFunctionalInterfaceMethodSignature().equals("()V") && serializedLambda.getImplClass().equals("Test") && serializedLambda.getImplMethodSignature().equals("()V"))
                                    synthetic2<caret>();
                                if (serializedLambda.getImplMethodKind() == 6 && serializedLambda.getFunctionalInterfaceClass().equals("Test${'$'}J") && serializedLambda.getFunctionalInterfaceMethodName().equals("foo") && serializedLambda.getFunctionalInterfaceMethodSignature().equals("()V") && serializedLambda.getImplClass().equals("Test") && serializedLambda.getImplMethodSignature().equals("()V"))
                                    synthetic1();
                                break;
                        }
                        throw new IllegalArgumentException("Invalid lambda deserialization");
                    }
                
                    I test1() {
                        return (I & Serializable) Test.synthetic2();
                    }
                
                    J test2() {
                        return (J & Serializable) Test.synthetic1();
                    }
                
                    private static void method() {
                    }
                
                    @FunctionalInterface
                    interface I {
                        void foo();
                    }
                
                    @FunctionalInterface
                    interface J {
                        void foo();
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "foo",
                "()LTest\$I;",
                altMetafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "method",
                    "()V",
                    false
                ),
                Type.getMethodType("()V"),
                LambdaMetafactory.FLAG_SERIALIZABLE or LambdaMetafactory.FLAG_BRIDGES,
                0,
            )
        )
    }

    @Test
    fun testSerializableLambdaWithCapture() {
        doIndyTest(
            """
                import java.io.Serializable;

                class Test {
                    void test(String capture1, int capture2, Object capture3) {
                        var r = (Runnable & Serializable) () -> System.out.println(capture1 + capture2 + capture3);
                    }
                }
            """.trimIndent(),
            """
                import java.io.Serializable;
                import java.lang.invoke.SerializedLambda;
                
                class Test {
                    private static void lambda${'$'}test$96740b79$1(String capture1, int capture2, Object capture3) {
                        System.out.println(capture1 + capture2 + capture3);
                    }
                
                    private static Runnable synthetic1(String param1, int param2, Object param3) {
                    }
                
                    private static Object ${'$'}deserializeLambda$(SerializedLambda serializedLambda) {
                        switch (serializedLambda.getImplMethodName()) {
                            case "lambda${'$'}test$96740b79$1":
                                if (serializedLambda.getImplMethodKind() == 6 && serializedLambda.getFunctionalInterfaceClass().equals("java/lang/Runnable") && serializedLambda.getFunctionalInterfaceMethodName().equals("run") && serializedLambda.getFunctionalInterfaceMethodSignature().equals("()V") && serializedLambda.getImplClass().equals("Test") && serializedLambda.getImplMethodSignature().equals("(Ljava/lang/String;ILjava/lang/Object;)V"))
                                    synthetic1((String) serializedLambda.getCapturedArg(0), (Integer) serializedLambda.getCapturedArg(1), serializedLambda.getCapturedArg(2));
                                break;
                        }
                        throw new IllegalArgumentException("Invalid lambda deserialization");
                    }
                
                    void test(String capture1, int capture2, Object capture3) {
                        var r = (Runnable & Serializable) Test.synthetic1<caret>(capture1, capture2, capture3);
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "run",
                "(Ljava/lang/String;ILjava/lang/Object;)Ljava/lang/Runnable;",
                altMetafactory,
                Type.getMethodType("()V"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "lambda\$test$96740b79$1",
                    "(Ljava/lang/String;ILjava/lang/Object;)V",
                    false
                ),
                Type.getMethodType("()V"),
                LambdaMetafactory.FLAG_SERIALIZABLE or LambdaMetafactory.FLAG_BRIDGES,
                0,
            )
        )
    }

    @Test
    fun testLambdaBridgeMethods() {
        doIndyTest(
            """
                class Test {
                    void test() {
                        K k = () -> "";
                    }
                    
                    @FunctionalInterface
                    interface I {
                        String foo();
                    }
                    
                    @FunctionalInterface
                    interface J {
                        Object foo();
                    }
                    
                    interface K extends I, J {
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    private static String lambda${'$'}test$0() {
                        return "";
                    }
                
                    private static K synthetic1() {
                    }
                
                    void test() {
                        K k = Test.synthetic1<caret>();
                    }
                
                    @FunctionalInterface
                    interface I {
                        String foo();
                    }
                
                    @FunctionalInterface
                    interface J {
                        Object foo();
                    }
                
                    interface K extends I, J {
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "foo",
                "()LTest\$K;",
                altMetafactory,
                Type.getMethodType("()Ljava/lang/String;"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "lambda\$test$0",
                    "()Ljava/lang/String;",
                    false
                ),
                Type.getMethodType("()Ljava/lang/String;"),
                LambdaMetafactory.FLAG_BRIDGES,
                1,
                Type.getMethodType("()Ljava/lang/Object;"),
            )
        )
    }

    @Test
    fun testLambdaGenericMethod() {
        doIndyTest(
            """
                import java.util.function.Consumer;
                
                class Test<T> {
                    <U> void test(T captured1, U captured2) {
                        Consumer<U> c = u -> System.out.println("" + captured1 + captured2);
                    }
                }
            """.trimIndent(),
            """
                import java.util.function.Consumer;
                
                class Test<T> {
                    private static <U> void lambda${'$'}test$0(T captured1, U captured2, U u) {
                        System.out.println("" + captured1 + captured2);
                    }
                
                    private static Consumer synthetic1(Object param1, Object param2) {
                    }
                
                    <U> void test(T captured1, U captured2) {
                        Consumer<U> c = Test.synthetic1<caret>(captured1, captured2);
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "accept",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/function/Consumer;",
                metafactory,
                Type.getMethodType("(Ljava/lang/Object;)V"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "lambda\$test$0",
                    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
                    false
                ),
                Type.getMethodType("(Ljava/lang/Object;)V"),
            )
        )
    }

    @Test
    fun testLambdaIntersectionParameter() {
        doTest(
            """
                import java.io.Serializable;

                class Test {
                    void test() {
                        I i = t -> {
                        };
                    }
                
                    @FunctionalInterface
                    interface I {
                        <T extends Runnable & Serializable> void accept(T t);
                    }
                }
            """.trimIndent(),
            """
                import java.io.Serializable;
                
                class Test {
                    private static <T extends Runnable & Serializable> void lambda${'$'}test$0(T t) {
                    }
                
                    private static I synthetic1() {
                    }
                
                    void test() {
                        I i = Test.synthetic1();
                    }
                
                    @FunctionalInterface
                    interface I {
                        <T extends Runnable & Serializable> void accept(T t);
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLambdaWildcard() {
        doTest(
            """
                import java.util.function.Supplier;

                class Test {
                    void test() {
                        method(() -> "");
                    }
                    
                    void method(Supplier<? super String> s) {
                    }
                }
            """.trimIndent(),
            """
                import java.util.function.Supplier;
                
                class Test {
                    private static Object lambda${'$'}test$0() {
                        return "";
                    }
                
                    private static Supplier synthetic1() {
                    }
                
                    void test() {
                        method(Test.synthetic1());
                    }
                
                    void method(Supplier<? super String> s) {
                    }
                }
            """.trimIndent()
        )
    }
}
