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

import com.demonwav.mcdev.platform.mixin.handlers.desugar.AnonymousAndLocalClassDesugarer
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes

@Suppress("Convert2Lambda", "CopyConstructorMissesField", "SillyAssignment")
class AnonymousAndLocalClassDesugarTest : AbstractDesugarTest() {
    override val desugarer = AnonymousAndLocalClassDesugarer

    @Test
    fun testSimpleAnonymous() {
        doTest(
            """
                class Test {
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("Hello World!");
                        }
                    };
                }
            """.trimIndent(),
            """
                class Test {
                    Runnable r = new $1();
                    
                    class $1 implements Runnable {
                        @Override
                        public void run() {
                            System.out.println("Hello World!");
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testAnonymousWithParameters() {
        doTest(
            """
                public class Test {
                    Test test = new Test(this) {
                    };
                    
                    public Test(Test test) {
                    }
                }
            """.trimIndent(),
            """
                public class Test {
                    Test test = new $1(this);
                    
                    public Test(Test test) {
                    }
                    
                    class $1 extends Test {
                        $1(Test test1) {
                            super(test1);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testAnonymousWithCapture() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello, World!";
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                System.out.println(hello);
                            }
                        };
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello, World!";
                        Runnable r = new $1(hello);
                    }
                    
                    class $1 implements Runnable {
                        final String val${'$'}hello;
                    
                        $1(String hello) {
                            this.val${'$'}hello = hello;
                            super();
                        }
                        
                        @Override
                        public void run() {
                            System.out.println(val${'$'}hello);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testAnonymousWithParameterAndCapture() {
        doTest(
            """
                public class Test {
                    void test() {
                        String hello = "Hello, World!";
                        Test test = new Test(this) {
                            String x = hello;
                        };
                    }
                    
                    public Test(Test test) {
                    }
                }
            """.trimIndent(),
            """
                public class Test {
                    void test() {
                        String hello = "Hello, World!";
                        Test test = new $1(this, hello);
                    }
                    
                    public Test(Test test) {
                    }
                    
                    class $1 extends Test {
                        final String val${'$'}hello;
                        String x = val${'$'}hello;
                        
                        $1(Test test1, String hello) {
                            this.val${'$'}hello = hello;
                            super(test1);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testStaticAnonymous() {
        doTest(
            """
                class Test {
                    static void test() {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                            }
                        };
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    static void test() {
                        Runnable r = new $1();
                    }
                    
                    static class $1 implements Runnable {
                        @Override
                        public void run() {
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testStaticAnonymousAsDelegateConstructorParameter() {
        doTest(
            """
                class Test {
                    public Test() {
                        this(new Object() {
                        });
                    }
                
                    public Test(Object x) {
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    public Test() {
                        this(new $1());
                    }
                    
                    public Test(Object x) {
                    }
                    
                    static class $1 {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testSimpleLocal() {
        doTest(
            """
                class Test {
                    void test() {
                        class Local {
                        }
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                    }
                    
                    class $1Local {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testSimpleLocalWithCapture() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello, World!";
                        class Local {
                            void print() {
                                System.out.println(hello);
                            }
                        }
                        new Local().print();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello, World!";
                        new $1Local(hello).print();
                    }
                    
                    class $1Local {
                        final String val${'$'}hello;
                        
                        $1Local(String hello) {
                            this.val${'$'}hello = hello;
                            super();
                        }
                        
                        void print() {
                            System.out.println(val${'$'}hello);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLocalWithExistingConstructorAndCapture() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        class Local {
                            private final String name;
                            
                            public Local(String name) {
                                this.name = name;
                            }
                            
                            void print() {
                                System.out.println(hello + " " + name);
                            }
                        }
                        new Local("World").print();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        new $1Local("World", hello).print();
                    }
                    
                    class $1Local {
                        final String val${'$'}hello;
                        private final String name;
                        
                        public $1Local(String name, String hello) {
                            this.val${'$'}hello = hello;
                            super();
                            this.name = name;
                        }
                        
                        void print() {
                            System.out.println(val${'$'}hello + " " + name);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLocalWithMultipleExistingConstructorsAndCapture() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        class Local {
                            private final String name;
                            
                            public Local() {
                                this.name = "World";
                            }
                            
                            public Local(String name) {
                                this.name = name;
                            }
                            
                            void print() {
                                System.out.println(hello + " " + name);
                            }
                        }
                        new Local().print();
                        new Local("World").print();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        new $1Local(hello).print();
                        new $1Local("World", hello).print();
                    }
                    
                    class $1Local {
                        final String val${'$'}hello;
                        private final String name;
                        
                        public $1Local(String hello) {
                            this.val${'$'}hello = hello;
                            super();
                            this.name = "World";
                        }
                        
                        public $1Local(String name, String hello) {
                            this.val${'$'}hello = hello;
                            super();
                            this.name = name;
                        }
                        
                        void print() {
                            System.out.println(val${'$'}hello + " " + name);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLocalWithDelegateConstructorAndCapture() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        class Local {
                            private final String name;
                        
                            public Local() {
                                this("World");
                            }
                            
                            public Local(String name) {
                                this.name = name;
                            }
                            
                            void print() {
                                System.out.println(hello + " " + name);
                            }
                        }
                        new Local().print();
                        new Local("World").print();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        new $1Local(hello).print();
                        new $1Local("World", hello).print();
                    }
                    
                    class $1Local {
                        final String val${'$'}hello;
                        private final String name;
                        
                        public $1Local(String hello) {
                            this("World", hello);
                        }
                        
                        public $1Local(String name, String hello) {
                            this.val${'$'}hello = hello;
                            super();
                            this.name = name;
                        }
                        
                        void print() {
                            System.out.println(val${'$'}hello + " " + name);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testAnonymousExtendsLocal() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        String world = "World";
                        class Local {
                            void print() {
                                System.out.println(hello);
                            }
                        }
                        new Local() {
                            @Override
                            void print() {
                                super.print();
                                System.out.println(world);
                            }
                        }.print();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        String world = "World";
                        new $1(hello, world).print();
                    }
                    
                    class $1Local {
                        final String val${'$'}hello;
                        
                        $1Local(String hello) {
                            this.val${'$'}hello = hello;
                            super();
                        }
                        
                        void print() {
                            System.out.println(val${'$'}hello);
                        }
                    }
                    
                    class $1 extends $1Local {
                        final String val${'$'}hello;
                        final String val${'$'}world;
                        
                        $1(String hello1, String world) {
                            this.val${'$'}hello = hello1;
                            this.val${'$'}world = world;
                            super(hello1);
                        }
                        
                        @Override
                        void print() {
                            super.print();
                            System.out.println(val${'$'}world);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testAnonymousExtendsLocalWithParameters() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        String world = "World";
                        class Local {
                            private final String name;
                            
                            public Local(String name) {
                                this.name = name;
                            }
                            
                            void print() {
                                System.out.println(hello + " " + name);
                            }
                        }
                        new Local(hello) {
                            @Override
                            void print() {
                                super.print();
                                System.out.println(hello + " " + world);
                            }
                        }.print();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        String world = "World";
                        new $1(hello, hello, world).print();
                    }
                    
                    class $1Local {
                        final String val${'$'}hello;
                        private final String name;
                        
                        public $1Local(String name, String hello) {
                            this.val${'$'}hello = hello;
                            super();
                            this.name = name;
                        }
                        
                        void print() {
                            System.out.println(val${'$'}hello + " " + name);
                        }
                    }
                    
                    class $1 extends $1Local {
                        final String val${'$'}hello;
                        final String val${'$'}world;
                        
                        $1(String name, String hello1, String world) {
                            this.val${'$'}hello = hello1;
                            this.val${'$'}world = world;
                            super(name, hello1);
                        }
                        
                        @Override
                        void print() {
                            super.print();
                            System.out.println(val${'$'}hello + " " + val${'$'}world);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLocalExtendsLocal() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        String world = "World";
                        class Local1 {
                            void print() {
                                System.out.println(hello);
                            }
                        }
                        class Local2 extends Local1 {
                            @Override
                            void print() {
                                super.print();
                                System.out.println(world);
                            }
                        }
                        new Local2().print();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        String world = "World";
                        new $1Local2(hello, world).print();
                    }
                    
                    class $1Local1 {
                        final String val${'$'}hello;
                        
                        $1Local1(String hello) {
                            this.val${'$'}hello = hello;
                            super();
                        }
                        
                        void print() {
                            System.out.println(val${'$'}hello);
                        }
                    }
                    
                    class $1Local2 extends $1Local1 {
                        final String val${'$'}hello;
                        final String val${'$'}world;
                        
                        $1Local2(String hello, String world) {
                            this.val${'$'}hello = hello;
                            this.val${'$'}world = world;
                            super(hello);
                        }
                        
                        @Override
                        void print() {
                            super.print();
                            System.out.println(val${'$'}world);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLocalExtendsLocalWithParameters() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        String world = "World";
                        
                        class Local1 {
                            private final String name;
                            
                            public Local1(String name) {
                                this.name = name;
                            }
                            
                            void print() {
                                System.out.println(hello + " " + name);
                            }
                        }
                        
                        class Local2 extends Local1 {
                            public Local2(String name) {
                                super(name);
                            }
                            
                            @Override
                            void print() {
                                super.print();
                                System.out.println(hello + " " + world);
                            }
                        }
                        
                        new Local2(hello).print();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        String world = "World";
                
                        new $1Local2(hello, hello, world).print();
                    }
                
                    class $1Local1 {
                        final String val${'$'}hello;
                        private final String name;
                
                        public $1Local1(String name, String hello) {
                            this.val${'$'}hello = hello;
                            super();
                            this.name = name;
                        }
                
                        void print() {
                            System.out.println(val${'$'}hello + " " + name);
                        }
                    }
                
                    class $1Local2 extends $1Local1 {
                        final String val${'$'}hello;
                        final String val${'$'}world;
                
                        public $1Local2(String name, String hello, String world) {
                            this.val${'$'}hello = hello;
                            this.val${'$'}world = world;
                            super(name, hello);
                        }
                
                        @Override
                        void print() {
                            super.print();
                            System.out.println(val${'$'}hello + " " + val${'$'}world);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testAnonymousInsideGenericClass() {
        doTest(
            """
                class Test<T> {
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                        }
                        
                        T test() {
                        }
                    };
                }
            """.trimIndent(),
            """
                class Test<T> {
                    Runnable r = new $1();
                
                    class $1 implements Runnable {
                        @Override
                        public void run() {
                        }
                        
                        T test() {
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testAnonymousInsideUnsuedGenericMethod() {
        doTest(
            """
                class Test {
                    <T> void test() {
                        new Object() {
                        };
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    <T> void test() {
                        new $1();
                    }
                
                    class $1 {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testAnonymousInsideGenericMethod() {
        doTest(
            """
                class Test {
                    <T> void test() {
                        new Object() {
                            T test() {
                            }
                        };
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    <T> void test() {
                        new $1<T>();
                    }
                
                    class $1<T> {
                        T test() {
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLocalInsideGenericClass() {
        doTest(
            """
                class Test<T> {
                    void test() {
                        class Local {
                            T test() {
                            }
                        }
                        new Local();
                    }
                }
            """.trimIndent(),
            """
                class Test<T> {
                    void test() {
                        new $1Local();
                    }
                
                    class $1Local {
                        T test() {
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLocalInsideUnsuedGenericMethod() {
        doTest(
            """
                class Test {
                    <T> void test() {
                        class Local {
                        }
                        new Local();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    <T> void test() {
                        new $1Local();
                    }
                
                    class $1Local {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testLocalInsideGenericMethod() {
        doTest(
            """
                class Test {
                    <T> void test() {
                        class Local {
                            T test() {
                            }
                        }
                        new Local();
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    <T> void test() {
                        new $1Local<T>();
                    }
                
                    class $1Local<T> {
                        T test() {
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testAnonymousExtendsGenericLocal() {
        doTest(
            """
                class Test {
                    void test() {
                        class Local<T> {
                        }
                        Local<String> local = new Local<>() {
                        };
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        $1Local<String> local = new $1();
                    }
                
                    class $1Local<T> {
                    }
                
                    class $1 extends $1Local<String> {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testCaptureUsedInConstructor() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        
                        class Local {
                            Local() {
                                System.out.println(hello);
                            }
                            
                            Local(String s) {
                                this();
                                System.out.println(hello);
                            }
                            
                            Local(int i) {
                                this(hello);
                            }
                        }
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                
                    }
                
                    class $1Local {
                        final String val${'$'}hello;
                
                        $1Local(String hello) {
                            this.val${'$'}hello = hello;
                            super();
                            System.out.println(val${'$'}hello);
                        }
                
                        $1Local(String s, String hello) {
                            this(hello);
                            System.out.println(val${'$'}hello);
                        }
                
                        $1Local(int i, String hello) {
                            this(hello, hello);
                        }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testCaptureUsedInConstructor22() {
        doTest(
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                        
                        class Local {
                            Local() {
                                System.out.println(hello);
                            }
                            
                            Local(String s) {
                                this();
                                System.out.println(hello);
                            }
                            
                            Local(int i) {
                                this(hello);
                            }
                        }
                    }
                }
            """.trimIndent(),
            """
                class Test {
                    void test() {
                        String hello = "Hello";
                
                    }
                
                    class $1Local {
                        final String val${'$'}hello;
                
                        $1Local(String hello) {
                            this.val${'$'}hello = hello;
                            super();
                            System.out.println(hello);
                        }
                
                        $1Local(String s, String hello) {
                            this(hello);
                            System.out.println(hello);
                        }
                
                        $1Local(int i, String hello) {
                            this(hello, hello);
                        }
                    }
                }
            """.trimIndent(),
            classVersion = Opcodes.V22
        )
    }
}
