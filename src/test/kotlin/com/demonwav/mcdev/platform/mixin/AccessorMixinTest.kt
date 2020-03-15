/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.util.isAccessorMixin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Accessor Mixin Extension Property Tests")
class AccessorMixinTest : BaseMixinTest() {

    @BeforeEach
    fun setupProject() {
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

    @Test
    @DisplayName("Valid Accessor Mixin Test")
    fun validAccessorMixinTest() = doTest("AccessorMixin", """
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
        Assertions.assertTrue(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Missing Annotation Accessor Mixin Test")
    fun missingAnnotationAccessorMixinTest() = doTest("MissingAnnotationAccessorMixin", """
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
        Assertions.assertFalse(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Target Interface Accessor Mixin Test")
    fun targetInterfaceAccessorMixinTest() = doTest("TargetInterface", """
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
        Assertions.assertFalse(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Accessors Only Accessor Mixin Test")
    fun accessorsOnlyAccessorMixinTest() = doTest("AccessorMixin", """
        package test;

        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public interface AccessorMixin {
            @Accessor String getString();
            @Accessor int getInt();
        }
    """) { psiClass ->
        Assertions.assertTrue(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Invokers Only Accessor Mixin Test")
    fun invokersOnlyAccessorMixinTest() = doTest("AccessorMixin", """
        package test;

        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public interface AccessorMixin {
            @Invoker void invoke();
            @Invoker double doThing();
        }
    """) { psiClass ->
        Assertions.assertTrue(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Non-Interface Accessor Mixin Test")
    fun nonInterfaceAccessorMixinTest() = doTest("NonInterfaceAccessorMixin", """
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
        Assertions.assertFalse(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Non-Interface Targeting Interface Accessor Mixin Test")
    fun nonInterfaceTargetingInterfaceAccessorMixinTest() = doTest("NonInterfaceTargetInterface", """
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
        Assertions.assertFalse(psiClass.isAccessorMixin)
    }
}
