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
import com.demonwav.mcdev.platform.mixin.inspection.reference.UnnecessaryQualifiedMemberReferenceInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Unnecessary Qualified Member Reference Inspection Tests")
class UnnecessaryQualifiedMemberReferenceInspectionTest : BaseMixinTest() {

    private fun doTest(@Language("JAVA") code: String) {
        buildProject {
            dir("test") {
                java("UnnecessaryQualifiedMemberReferenceMixin.java", code)
            }
        }

        fixture.enableInspections(UnnecessaryQualifiedMemberReferenceInspection::class)
        fixture.checkHighlighting(false, false, true)
    }

    @Test
    @DisplayName("Unnecessary Qualification")
    fun unnecessaryQualification() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.unnecessaryQualifiedMemberReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class UnnecessaryQualifiedMemberReferenceMixin {
            
                @Inject(method = <weak_warning descr="Unnecessary qualified reference to 'method' in target class">"Ltest/MixedIn;method"</weak_warning>, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("No unnecessary Qualification")
    fun noUnnecessaryQualification() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.unnecessaryQualifiedMemberReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class UnnecessaryQualifiedMemberReferenceMixin {
            
                @Inject(method = "method", at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("Unnecessary Qualification Multiple Targets")
    fun unnecessaryQualificationMultipleTargets() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.unnecessaryQualifiedMemberReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class UnnecessaryQualifiedMemberReferenceMixin {
            
                @Inject(method = {<weak_warning descr="Unnecessary qualified reference to 'method' in target class">"Ltest/MixedIn;method"</weak_warning>, "otherMethod"}, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("No Unnecessary Qualification Multiple Targets")
    fun noUnnecessaryQualificationMultipleTargets() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.unnecessaryQualifiedMemberReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class UnnecessaryQualifiedMemberReferenceMixin {
            
                @Inject(method = {"method", "otherMethod"}, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }
}
