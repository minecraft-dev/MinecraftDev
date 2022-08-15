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
import com.demonwav.mcdev.platform.mixin.inspection.MixinSuperClassInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Mixin Super Class Inspection Tests")
class SuperClassTest : BaseMixinTest() {

    private fun doTest(
        @Language("JAVA")
        mixinCode: String
    ) {
        buildProject {
            dir("test") {
                java("SuperClassMixin.java", mixinCode)
            }
        }

        fixture.enableInspections(MixinSuperClassInspection::class)
        fixture.checkHighlighting(true, false, false)
    }

    @Test
    @DisplayName("No Mixin Superclass Test")
    fun noMixinSuperclassTest() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.superClass.Minecrell;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(Minecrell.class)
            public class SuperClassMixin {

            }
            """
        )
    }

    @Test
    @DisplayName("Good Mixin Superclass Test")
    fun goodMixinSuperclassTest() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.superClass.Entity;
            import com.demonwav.mcdev.mixintestdata.superClass.Minecrell;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(Minecrell.class)
            public class SuperClassMixin extends Entity {

            }
            """
        )
    }

    @Test
    @DisplayName("Mixin Class Cannot Extend Itself Test")
    fun mixinClassNotItsOwnSuperclassTest() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.superClass.DemonWav;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(DemonWav.class)
            public class SuperClassMixin extends <error descr="Cannot extend target class">DemonWav</error> {

            }
            """
        )
    }

    @Test
    @DisplayName("Mixin Superclass Not Found In Hierarchy Test")
    fun mixinClassHierarchyNotFoundTest() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.superClass.DemonWav;
            import com.demonwav.mcdev.mixintestdata.superClass.Minecrell;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(DemonWav.class)
            public class SuperClassMixin extends <error descr="Cannot find 'Minecrell' in the hierarchy of target class 'DemonWav'">Minecrell</error> {

            }
            """
        )
    }
}
