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
import com.demonwav.mcdev.platform.mixin.handlers.desugar.MethodReferenceToLambdaDesugarer
import org.junit.jupiter.api.Test
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

@Suppress("rawtypes")
class MethodReferenceAndLambdaDesugarTest : AbstractDesugarMultiTest() {
    override val desugarers = listOf(
        MethodReferenceToLambdaDesugarer,
        LambdaDesugarer,
    )

    @Test
    fun testQualifiedMethodReference() {
        doIndyTest(
            """
                import java.util.function.Consumer;

                class Test {
                    static void test() {
                        Consumer<String> c = new Test()::method;
                    }
                    
                    private void method(String... args) {
                    }
                }
            """.trimIndent(),
            """
                import java.util.function.Consumer;
                
                class Test {
                    static void test() {
                        Consumer<String> c = Test.synthetic1<caret>(new Test());
                    }
                
                    private static void lambda${'$'}test$0(Test self, String args) {
                        self.method(args);
                    }
                
                    private static Consumer synthetic1(Test param1) {
                    }
                
                    private void method(String... args) {
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "accept",
                "(LTest;)Ljava/util/function/Consumer;",
                LambdaDesugarTest.metafactory,
                Type.getMethodType("(Ljava/lang/Object;)V"),
                Handle(
                    Opcodes.H_INVOKESTATIC,
                    "Test",
                    "lambda\$test$0",
                    "(LTest;Ljava/lang/String;)V",
                    false
                ),
                Type.getMethodType("(Ljava/lang/String;)V"),
            )
        )
    }

    @Test
    fun testSuperMethodReference() {
        doIndyTest(
            """
                import java.util.function.Supplier;
                
                class Test {
                    void test() {
                        Supplier<Object> s = super::clone;
                    }
                }
            """.trimIndent(),
            """
                import java.util.function.Supplier;
                
                class Test {
                    private static Supplier synthetic1(Test param1) {
                    }
                
                    void test() {
                        Supplier<Object> s = Test.synthetic1<caret>(this);
                    }
                
                    private Object lambda${'$'}test$0() {
                        return super.clone();
                    }
                }
            """.trimIndent(),
            DesugarUtil.IndyData(
                "get",
                "(LTest;)Ljava/util/function/Supplier;",
                LambdaDesugarTest.metafactory,
                Type.getMethodType("()Ljava/lang/Object;"),
                Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    "Test",
                    "lambda\$test$0",
                    "()Ljava/lang/Object;",
                    false
                ),
                Type.getMethodType("()Ljava/lang/Object;"),
            )
        )
    }
}
