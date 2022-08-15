/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationTargetInspection
import com.demonwav.mcdev.platform.mixin.util.isAccessorMixin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Accessor Mixin Extension Property Tests")
class AccessorMixinTest : BaseMixinTest() {

    private fun doTest(className: String, @Language("JAVA") code: String, test: (psiClass: PsiClass) -> Unit) {
        var psiClass: PsiClass? = null
        buildProject {
            dir("test") {
                psiClass = java("$className.java", code).toPsiFile<PsiJavaFile>().classes[0]
            }
        }

        test(psiClass!!)
    }

    @Test
    @DisplayName("Valid Accessor Mixin Test")
    fun validAccessorMixinTest() = doTest(
        "AccessorMixin",
        """
        package test;

        import com.demonwav.mcdev.mixintestdata.accessor.BaseMixin;
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
        """
    ) { psiClass ->
        Assertions.assertTrue(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Missing Annotation Accessor Mixin Test")
    fun missingAnnotationAccessorMixinTest() = doTest(
        "MissingAnnotationAccessorMixin",
        """
        package test;

        import com.demonwav.mcdev.mixintestdata.accessor.BaseMixin;
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
        """
    ) { psiClass ->
        Assertions.assertFalse(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Target Interface Accessor Mixin Test")
    fun targetInterfaceAccessorMixinTest() = doTest(
        "TargetInterface",
        """
        package test;

        import com.demonwav.mcdev.mixintestdata.accessor.BaseMixinInterface;
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
        """
    ) { psiClass ->
        Assertions.assertFalse(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Accessors Only Accessor Mixin Test")
    fun accessorsOnlyAccessorMixinTest() = doTest(
        "AccessorMixin",
        """
        package test;

        import com.demonwav.mcdev.mixintestdata.accessor.BaseMixin;
        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public interface AccessorMixin {
            @Accessor String getString();
            @Accessor int getInt();
        }
        """
    ) { psiClass ->
        Assertions.assertTrue(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Invokers Only Accessor Mixin Test")
    fun invokersOnlyAccessorMixinTest() = doTest(
        "AccessorMixin",
        """
        package test;

        import com.demonwav.mcdev.mixintestdata.accessor.BaseMixin;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public interface AccessorMixin {
            @Invoker void invoke();
            @Invoker double doThing();
        }
        """
    ) { psiClass ->
        Assertions.assertTrue(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Non-Interface Accessor Mixin Test")
    fun nonInterfaceAccessorMixinTest() = doTest(
        "NonInterfaceAccessorMixin",
        """
        package test;

        import com.demonwav.mcdev.mixintestdata.accessor.BaseMixin;
        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixin.class)
        public class NonInterfaceAccessorMixin {
            @Accessor String getString();
            @Invoker void invoke();
        }
        """
    ) { psiClass ->
        Assertions.assertFalse(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Non-Interface Targeting Interface Accessor Mixin Test")
    fun nonInterfaceTargetingInterfaceAccessorMixinTest() = doTest(
        "NonInterfaceTargetInterface",
        """
        package test;

        import com.demonwav.mcdev.mixintestdata.accessor.BaseMixinInterface;
        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;

        @Mixin(BaseMixinInterface.class)
        public class NonInterfaceAccessorMixin {
            @Accessor String getString();
            @Invoker void invoke();
        }
        """
    ) { psiClass ->
        Assertions.assertFalse(psiClass.isAccessorMixin)
    }

    @Test
    @DisplayName("Accessor Mixin Target Test")
    fun accessorMixinTargetTest() = doTest(
        "AccessorMixinTargetMixin",
        """
        package test;
        
        import com.demonwav.mcdev.mixintestdata.shadow.MixinBase;
        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;
        import org.spongepowered.asm.mixin.Mutable;
        
        @Mixin(MixinBase.class)
        public interface AccessorMixinTargetMixin {
            @Accessor static String getPrivateStaticString() { return null; }
            @Accessor static void setPrivateStaticString(String value) {}
            @Accessor String getPrivateString();
            @Accessor void setPrivateString(String value);
            @Accessor @Mutable void setPrivateFinalString(String value);
            @Invoker static String callPrivateStaticMethod() { return null; }
            @Invoker String callPrivateMethod();
            @Invoker static MixinBase createMixinBase() { return null; }
        }
        """
    ) {
        fixture.enableInspections(MixinAnnotationTargetInspection::class.java)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Accessor Mixin Renamed Target Test")
    fun accessorMixinRenamedTargetTest() = doTest(
        "AccessorMixinRenamedTargetMixin",
        """
        package test;
        
        import com.demonwav.mcdev.mixintestdata.shadow.MixinBase;
        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;
        
        @Mixin(MixinBase.class)
        public interface AccessorMixinRenamedTargetMixin {
            @Accessor("privateString") String foo1();
            @Accessor("privateString") void foo2(String value);
            @Invoker("privateMethod") String foo3();
            @Invoker("<init>") MixinBase foo4();
        }
        """
    ) {
        fixture.enableInspections(MixinAnnotationTargetInspection::class.java)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Invalid Accessor Target")
    fun invalidAccessorTarget() = doTest(
        "AccessorMixinTargetMixin",
        """
        package test;
        
        import com.demonwav.mcdev.mixintestdata.shadow.MixinBase;
        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.Mixin;
        
        @Mixin(MixinBase.class)
        public interface AccessorMixinTargetMixin {
            @<error descr="Cannot find field foo in target class">Accessor</error> String getFoo();
        }
        """
    ) {
        fixture.enableInspections(MixinAnnotationTargetInspection::class.java)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Invalid Named Accessor Target")
    fun invalidNamedAccessorTarget() = doTest(
        "AccessorMixinTargetMixin",
        """
        package test;
        
        import com.demonwav.mcdev.mixintestdata.shadow.MixinBase;
        import org.spongepowered.asm.mixin.gen.Accessor;
        import org.spongepowered.asm.mixin.Mixin;
        
        @Mixin(MixinBase.class)
        public interface AccessorMixinTargetMixin {
            @<error descr="Cannot find field foo in target class">Accessor</error>("foo") String bar();
        }
        """
    ) {
        fixture.enableInspections(MixinAnnotationTargetInspection::class.java)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Invalid Invoker Target")
    fun invalidInvokerTarget() = doTest(
        "AccessorMixinTargetMixin",
        """
        package test;
        
        import com.demonwav.mcdev.mixintestdata.shadow.MixinBase;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;
        
        @Mixin(MixinBase.class)
        public interface AccessorMixinTargetMixin {
            @<error descr="Cannot find method foo in target class">Invoker</error> String callFoo();
        }
        """
    ) {
        fixture.enableInspections(MixinAnnotationTargetInspection::class.java)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Invalid Named Invoker Target")
    fun invalidNamedInvokerTarget() = doTest(
        "AccessorMixinTargetMixin",
        """
        package test;
        
        import com.demonwav.mcdev.mixintestdata.shadow.MixinBase;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;
        
        @Mixin(MixinBase.class)
        public interface AccessorMixinTargetMixin {
            @<error descr="Cannot find method foo in target class">Invoker</error>("foo") String bar();
        }
        """
    ) {
        fixture.enableInspections(MixinAnnotationTargetInspection::class.java)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Invalid Constructor Invoker Target")
    fun invalidConstructorInvokerTarget() = doTest(
        "AccessorMixinTargetMixin",
        """
        package test;
        
        import com.demonwav.mcdev.mixintestdata.shadow.MixinBase;
        import org.spongepowered.asm.mixin.gen.Invoker;
        import org.spongepowered.asm.mixin.Mixin;
        
        @Mixin(MixinBase.class)
        public interface AccessorMixinTargetMixin {
            @<error descr="Cannot find method <init> in target class">Invoker</error>("<init>") String construct(String invalidArg);
        }
        """
    ) {
        fixture.enableInspections(MixinAnnotationTargetInspection::class.java)
        fixture.checkHighlighting(false, false, false)
    }
}
