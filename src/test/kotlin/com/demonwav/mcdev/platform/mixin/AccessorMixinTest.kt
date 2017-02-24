/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.platform.mixin.util.isAccessorMixin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import org.intellij.lang.annotations.Language

class AccessorMixinTest : BaseMixinTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            src {
                java("test/BaseMixin.java", """
                    package test;
                    public class BaseMixin {}
                """)

                java("test/BaseMixinInterface.java", """
                    package test;
                    public interface BaseMixinInterface {}
                """)
            }
        }
    }

    private fun doTest(className: String, @Language("JAVA") code: String, test: (psiClass: PsiClass) -> Unit) {
        var psiClass: PsiClass? = null
        buildProject {
            src {
                psiClass = java("test/$className.java", code).toPsiFile<PsiJavaFile>().classes[0]
            }
        }

        test(psiClass!!)
    }

    fun testAccessorMixin() = doTest("AccessorMixin", """
        package test;

        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public interface AccessorMixin {
            @Accessor String getString();
            @Accessor int getInt();
            @Invoker void invoke();
            @Invoker double doThing();
        }
    """) { psiClass ->
        assertTrue(psiClass.isAccessorMixin)
    }

    fun testMissingAnnotation() = doTest("MissingAnnotationAccessorMixin", """
        package test;

        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public interface MissingAnnotationAccessorMixin {
            @Accessor String getString();
            @Accessor int getInt();
            @Invoker void invoke();
            double doThing();
        }
    """) { psiClass ->
        assertFalse(psiClass.isAccessorMixin)
    }

    fun testTargetInterface() = doTest("TargetInterface", """
        package test;

        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixinInterface.class)
        public interface TargetInterface {
            @Accessor String getString();
            @Accessor int getInt();
            @Invoker void invoke();
            @Invoker double doThing();
        }
    """) { psiClass ->
        assertFalse(psiClass.isAccessorMixin)
    }

    fun testOnlyAccessors() = doTest("AccessorMixin", """
        package test;

        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public interface AccessorMixin {
            @Accessor String getString();
            @Accessor int getInt();
        }
    """) { psiClass ->
        assertTrue(psiClass.isAccessorMixin)
    }

    fun testOnlyInvokers() = doTest("AccessorMixin", """
        package test;

        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public interface AccessorMixin {
            @Invoker void invoke();
            @Invoker double doThing();
        }
    """) { psiClass ->
        assertTrue(psiClass.isAccessorMixin)
    }

    fun testNonInterfaceAccessorMixin() = doTest("NonInterfaceAccessorMixin", """
        package test;

        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public class NonInterfaceAccessorMixin {
            @Accessor String getString();
            @Invoker void invoke();
        }
    """) { psiClass ->
        assertFalse(psiClass.isAccessorMixin)
    }

    fun testNonInterfaceTargetInterface() = doTest("NonInterfaceTargetInterface", """
        package test;

        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixinInterface.class)
        public class NonInterfaceAccessorMixin {
            @Accessor String getString();
            @Invoker void invoke();
        }
    """) { psiClass ->
        assertFalse(psiClass.isAccessorMixin)
    }
}
