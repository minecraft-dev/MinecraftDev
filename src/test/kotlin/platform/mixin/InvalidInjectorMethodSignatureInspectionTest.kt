/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.inspection.injector.InvalidInjectorMethodSignatureInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Invalid Injector Method Signature Inspection Test")
class InvalidInjectorMethodSignatureInspectionTest : BaseMixinTest() {

    private fun doTest(@Language("JAVA") code: String) {
        buildProject {
            dir("test") {
                java("TestMixin.java", code)
            }
        }

        fixture.enableInspections(InvalidInjectorMethodSignatureInspection::class)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Redirect in constructor before superconstructor call")
    fun redirectInConstructorBeforeSuperconstructorCall() {
        doTest(
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureInspection.MixedInOuter;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Redirect;
            
            @Mixin(MixedInOuter.class)
            public class TestMixin {
                @Redirect(method = "<init>()V", at = @At(value = "INVOKE", target = "Lcom/demonwav/mcdev/mixintestdata/invalidInjectorMethodSignatureInspection/MixedInOuter;method1()Ljava/lang/String;"))
                private String <error descr="Method must be static">redirectMethod1</error>() {
                    return null;
                }
                
                @Redirect(method = "<init>()V", at = @At(value = "INVOKE", target = "Lcom/demonwav/mcdev/mixintestdata/invalidInjectorMethodSignatureInspection/MixedInOuter;method2()V"))
                private void redirectMethod2() {
                }
            }
            """,
        )
    }

    @Test
    @DisplayName("Inner Ctor @Inject Parameters")
    fun innerCtorInjectParameters() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureInspection.MixedInOuter;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

            @Mixin(MixedInOuter.MixedInInner.class)
            public class TestMixin {

                @Inject(method = "<init>(Lcom/demonwav/mcdev/mixintestdata/invalidInjectorMethodSignatureInspection/MixedInOuter;)V", at = @At("RETURN"))
                private void injectCtor(MixedInOuter outer, CallbackInfo ci) {
                }

                @Inject(method = "<init>", at = @At("RETURN"))
                private void injectCtor(CallbackInfo ci) {
                }

                @Inject(method = "<init>(Lcom/demonwav/mcdev/mixintestdata/invalidInjectorMethodSignatureInspection/MixedInOuter;Ljava/lang/String;)V", at = @At("RETURN"))
                private void injectCtor(MixedInOuter outer, String string, CallbackInfo ci) {
                }

                @Inject(method = "<init>(Lcom/demonwav/mcdev/mixintestdata/invalidInjectorMethodSignatureInspection/MixedInOuter;Ljava/lang/String;)V", at = @At("RETURN"))
                private <error descr="Method signature does not match expected signature for Inject">void injectCtor(String string, CallbackInfo ci)</error> {
                }
            }
            """,
        )
    }

    @Test
    @DisplayName("Static Inner Ctor @Inject Parameters")
    fun staticInnerCtorInjectParameters() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureInspection.MixedInOuter;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

            @Mixin(MixedInOuter.MixedInStaticInner.class)
            public class TestMixin {

                @Inject(method = "<init>()V", at = @At("RETURN"))
                private <error descr="Method signature does not match expected signature for Inject">void injectCtorWrong(MixedInOuter outer, CallbackInfo ci)</error> {
                }

                @Inject(method = "<init>", at = @At("RETURN"))
                private void injectCtor(CallbackInfo ci) {
                }

                @Inject(method = "<init>(Ljava/lang/String;)V", at = @At("RETURN"))
                private <error descr="Method signature does not match expected signature for Inject">void injectCtor(MixedInOuter outer, String string, CallbackInfo ci)</error> {
                }

                @Inject(method = "<init>(Ljava/lang/String;)V", at = @At("RETURN"))
                private void injectCtor(String string, CallbackInfo ci) {
                }
            }
            """,
        )
    }
}
