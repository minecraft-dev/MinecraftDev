package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyExpressionValue;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.io.Serializable;

@Mixin(MixedInModifyExpressionValue.class)
class TestMixin {
    @Expression("42")
    @ModifyExpressionValue(method = "method1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test1() { // expect-template
    }

    @Expression("42")
    @ModifyExpressionValue(
            method = "method1",
            at = {@At("MIXINEXTRAS:EXPRESSION"), @At(value = "CONSTANT", args = "intValue=101")}
    )
    private void test2() {
    }

    @Definition(id = "callee1", method = "callee1")
    @Expression({"42", "callee1()"})
    @ModifyExpressionValue(method = "method1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test3() {
    }

    @Definition(id = "callee2", method = "callee2")
    @Expression({"42", "callee2()"})
    @ModifyExpressionValue(method = "method1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test4(@Coerce char original) {
    }

    @Definition(id = "callee2", method = "callee2")
    @Expression({"42", "callee2()"})
    @ModifyExpressionValue(method = "method1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test5(int shouldBePreserved) {
    }

    @Expression("'a'")
    @ModifyExpressionValue(method = "method2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test6(char shouldBePreserved, String a) {
    }

    @Expression("'a'")
    @ModifyExpressionValue(method = "method2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test7(byte shouldBePreserved, @Coerce Serializable a, long b) {
    }

    @ModifyExpressionValue(
            method = "method2",
            at = {@At(value = "INVOKE", target = "callee3"), @At(value = "INVOKE", target = "callee4")}
    )
    private void test8() {
    }

    @ModifyExpressionValue(
            method = "method2",
            at = {@At(value = "INVOKE", target = "callee3"), @At(value = "INVOKE", target = "callee4")}
    )
    private void test9(@Coerce Comparable<?>[] original) {
    }

    @Expression({"42", "101"})
    @ModifyExpressionValue(method = "method1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void test10() { // expect-template
    }
}
