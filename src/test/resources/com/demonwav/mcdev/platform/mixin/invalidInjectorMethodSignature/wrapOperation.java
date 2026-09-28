package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInWrapOperation;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(MixedInWrapOperation.class)
class TestMixin {
    @WrapOperation(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test1() {
    }

    @WrapOperation(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee3")}
    )
    private static void test2() {
    }

    @WrapOperation(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private String test3(@Coerce Object instance, @Coerce Object x, @Coerce char y, Operation<?> original) {
    }

    @WrapOperation(method = "caller", at = @At(value = "FIELD", target = "test"))
    private void test4() {
    }

    @WrapOperation(
            method = "caller",
            at = {@At(value = "INVOKE", target = "intValue"), @At(value = "FIELD", target = "test2", opcode = 180)}
    )
    private void test5() {
    }

    @WrapOperation(method = "caller", at = @At(value = "CONSTANT", args = "classValue=java/lang/Integer"))
    private void test6() {
    }

    @Expression("? == 3")
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test7() { // expect-template
    }

    @Expression({"? == 3", "? == 4"})
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test8() { // expect-template
    }

    @Definition(id = "callee4", method = "callee4")
    @Expression({"? == 3", "callee4(?, ?)"})
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test9() {
    }

    @Definition(id = "callee5", method = "callee5")
    @Expression({"? == 3", "callee5(?, ?)"})
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test10() {
    }

    @Definition(id = "callee6", method = "callee6")
    @Expression({"? == 3", "callee6(?, ?)"})
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test11() {
    }

    @Expression("? == 3")
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test12(int a, @Coerce boolean b, Operation<Boolean> original) {
    }
}