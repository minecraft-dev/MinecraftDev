/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.inspection.reference.InvalidMemberReferenceInspection
import com.demonwav.mcdev.platform.mixin.inspection.reference.UnresolvedReferenceInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * This is nothing more than a regression test to make sure this doesn't get broken in the future.
 */
@ExtendWith(EdtInterceptor::class)
@DisplayName("Multiple targets test")
class MultipleTargetTest : BaseMixinTest() {

    private fun doTest(@Language("JAVA") code: String) {
        buildProject {
            dir("test") {
                java(
                    "MixedIn.java",
                    """
                    package test;

                    class MixedIn {
                        public void method1() {
                        }
                        
                        public void method2() {
                        }
                    }
                    """,
                    configure = false
                )
                java("AmbiguousReferenceMixin.java", code)
            }
        }

        fixture.enableInspections(UnresolvedReferenceInspection::class, InvalidMemberReferenceInspection::class)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Single Target")
    fun singleTarget() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class MultipleTargetMixin {
            
                @Inject(method = "method1", at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("Multiple Targets")
    fun multipleTargets() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class MultipleTargetMixin {
            
                @Inject(method = {"method1", "method2"}, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }
}
