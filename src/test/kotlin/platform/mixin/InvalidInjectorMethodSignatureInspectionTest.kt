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

    @Test
    @DisplayName("Wildcard Inject with Single Target")
    fun wildcardInInjectWithSingleTarget() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureInspection.MixedInOuter;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;
            import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

            @Mixin(MixedInOuter.class)
            public class TestMixin {
                @Inject(method = "*", at = @At(value = "INVOKE", target = "Lcom/demonwav/mcdev/mixintestdata/invalidInjectorMethodSignatureInspection/MixedInOuter;method2()V"))
                private void test(CallbackInfo ci) {
                }
            }
            """,
        )
    }

    @Test
    @DisplayName("ModifyArgs")
    fun modifyArgs() {
        doTest(
            """
                package test;
                
                import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInSimple;
                import org.spongepowered.asm.mixin.Mixin;
                import org.spongepowered.asm.mixin.injection.At;
                import org.spongepowered.asm.mixin.injection.Coerce;
                import org.spongepowered.asm.mixin.injection.ModifyArgs;
                import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
                
                @Mixin(MixedInSimple.class)
                public class TestMixin {
                    @ModifyArgs(method = "simpleMethod", at = @At(value = "INVOKE", target = "parseInt"))
                    private void correct(Args args) {
                    }
                    
                    @ModifyArgs(method = "simpleMethod", at = @At(value = "INVOKE", target = "parseInt"))
                    private void correctWithTrailing(Args args, String string, int i) {
                    }
                    
                    @ModifyArgs(method = "simpleMethod", at = @At(value = "INVOKE", target = "parseInt"))
                    private <error descr="Method signature does not match expected signature for ModifyArgs">void prefixOfTrailing(Args args, String string)</error> {
                    }
                    
                    @ModifyArgs(method = "simpleMethod", at = @At(value = "INVOKE", target = "parseInt"))
                    private <error descr="Method signature does not match expected signature for ModifyArgs">void coerceTrailing(Args args, @Coerce Object string, int i)</error> {
                    }
                    
                    @ModifyArgs(method = "simpleMethod", at = @At(value = "INVOKE", target = "parseInt"))
                    private <error descr="Method signature does not match expected signature for ModifyArgs">void coerceArgs(@Coerce Object args)</error> {
                    }
                }
            """,
        )
    }

    @Test
    @DisplayName("ModifyArg")
    fun modifyArg() {
        doTest(
            """
                package test;
                
                import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyArg;
                import org.spongepowered.asm.mixin.Mixin;
                import org.spongepowered.asm.mixin.injection.At;
                import org.spongepowered.asm.mixin.injection.Coerce;
                import org.spongepowered.asm.mixin.injection.ModifyArg;
                
                @Mixin(MixedInModifyArg.class)
                public class TestMixin {
                    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
                    private String correct(String original) {
                        return original;
                    }
                    
                    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
                    private int correct(int original) {
                        return original;
                    }
                    
                    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
                    private String correctFull(String x, int y) {
                        return x;
                    }
                    
                    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee3"), index = 2)
                    private int correctIndex(int original) {
                        return original;
                    }
                    
                    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee3"), index = 2)
                    private <error descr="Method signature does not match expected signature for ModifyArg">int captureOuter(int original, Object obj)</error> {
                        return original;
                    }
                    
                    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee3"), index = 2)
                    private <error descr="Method signature does not match expected signature for ModifyArg">int captureOuter(int a, int b, int c, Object obj)</error> {
                        return c;
                    }
                    
                    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee3"))
                    private <error descr="There are no possible signatures for this injector">void implicit()</error> {
                    }
                    
                    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"), index = 1)
                    private @Coerce <error descr="Method signature does not match expected signature for ModifyArg">Object coerce(@Coerce Object x)</error> {
                        return x;
                    }
                }
            """,
        )
    }

    @Test
    @DisplayName("ModifyVariable")
    fun modifyVariable() {
        doTest(
            """
                package test;
                
                import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyVariable;
                import org.spongepowered.asm.mixin.Mixin;
                import org.spongepowered.asm.mixin.injection.At;
                import org.spongepowered.asm.mixin.injection.Coerce;
                import org.spongepowered.asm.mixin.injection.ModifyVariable;
                
                @Mixin(MixedInModifyVariable.class)
                public class TestMixin {
                    @ModifyVariable(method = "method1", at = @At("RETURN"))
                    private Integer correct(Integer original) {
                        return original;
                    }
                    
                    @ModifyVariable(method = "method1", at = @At("RETURN"))
                    private char correct(char original) {
                        return original;
                    }
                    
                    @ModifyVariable(method = "method1", at = @At("RETURN"))
                    private char correctPartialCapture(char original, String arg) {
                        return original;
                    }
                    
                    @ModifyVariable(method = "method1", at = @At("RETURN"))
                    private char correctFullCapture(char original, String arg, Object arg2) {
                        return original;
                    }
                    
                    @ModifyVariable(method = "method1", at = @At("RETURN"))
                    private char correctCoerceTrailing(char original, @Coerce Object arg) {
                        return original;
                    }
                    
                    @ModifyVariable(method = "method3", at = @At("RETURN"), ordinal = 2)
                    private int correctOrdinal(int original) {
                        return original;
                    }
                    
                    @ModifyVariable(method = "method2", at = @At("RETURN"))
                    private <error descr="There are no possible signatures for this injector">void implicit()</error> {
                    }
                    
                    @ModifyVariable(method = "method1", at = @At("RETURN"), index = 1)
                    private @Coerce <error descr="Method signature does not match expected signature for ModifyVariable">Object coerce(@Coerce Object x)</error> {
                        return x;
                    }
                }
            """,
        )
    }

    @Test
    @DisplayName("WrapMethod")
    fun wrapMethod() {
        doTest(
            """
                package test;
                
                import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInWrapMethod;
                import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
                import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
                import org.spongepowered.asm.mixin.Mixin;
                
                @Mixin(MixedInWrapMethod.class)
                class TestMixin {
                    @WrapMethod(method = {"method1", "method7"})
                    private void <error descr="Impossible combination of targets: some require a static handler and others a non-static handler">mustBeStaticAndNonStatic</error>(String arg, Operation<Void> original) {
                    }
                    
                    @WrapMethod(method = {"method7", "method8"})
                    private static <error descr="There are no possible signatures for this injector">void incompatibleShapes()</error> {
                    }
                }
            """,
        )
    }

    @Test
    @DisplayName("ModifyReturnValue")
    fun modifyReturnValue() {
        doTest(
            """
                package test;
                
                import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyReturnValue;
                import com.llamalad7.mixinextras.injector.ModifyReturnValue;
                import org.spongepowered.asm.mixin.Mixin;
                import org.spongepowered.asm.mixin.injection.At;
                
                @Mixin(MixedInModifyReturnValue.class)
                class TestMixin {
                    @ModifyReturnValue(method = {"method1", "method4"}, at = @At("RETURN"))
                    private <error descr="There are no possible signatures for this injector">void incompatibleShapes()</error> {
                    }
                }
            """,
        )
    }
}
