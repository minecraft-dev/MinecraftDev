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
    private int test1(int original) { // expect-template
        return original;
    }

    @Expression("42")
    @ModifyExpressionValue(
            method = "method1",
            at = {@At("MIXINEXTRAS:EXPRESSION"), @At(value = "CONSTANT", args = "intValue=101")}
    )
    private int test2(int original) {
        return original;
    }

    @Definition(id = "callee1", method = "callee1")
    @Expression({"42", "callee1()"})
    @ModifyExpressionValue(method = "method1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private char test3(char original) {
        return original;
    }

    @Definition(id = "callee2", method = "callee2")
    @Expression({"42", "callee2()"})
    @ModifyExpressionValue(method = "method1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private @Coerce char test4(@Coerce char original) {
        return original;
    }

    @Definition(id = "callee2", method = "callee2")
    @Expression({"42", "callee2()"})
    @ModifyExpressionValue(method = "method1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private int test5(int shouldBePreserved) {
        return shouldBePreserved;
    }

    @Expression("'a'")
    @ModifyExpressionValue(method = "method2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private char test6(char shouldBePreserved, String a) {
        return shouldBePreserved;
    }

    @Expression("'a'")
    @ModifyExpressionValue(method = "method2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private byte test7(byte shouldBePreserved, @Coerce Serializable a, long b) {
        return shouldBePreserved;
    }

    @ModifyExpressionValue(
            method = "method2",
            at = {@At(value = "INVOKE", target = "callee3"), @At(value = "INVOKE", target = "callee4")}
    )
    private @Coerce Number[] test8(@Coerce Number[] original) {
        return null;
    }

    @ModifyExpressionValue(
            method = "method2",
            at = {@At(value = "INVOKE", target = "callee3"), @At(value = "INVOKE", target = "callee4")}
    )
    private @Coerce Number[] test9(@Coerce Comparable<?>[] original) {
        return null;
    }

    @Expression({"42", "101"})
    @ModifyExpressionValue(method = "method1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private int test10(int original) { // expect-template
        return original;
    }
}