package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInWrapOperation;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.io.Serializable;

@Mixin(MixedInWrapOperation.class)
class TestMixin {
    @WrapOperation(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test1(MixedInWrapOperation instance, String x, int y, Operation<Void> original) {
    }

    @WrapOperation(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee3")}
    )
    private static void test2(MixedInWrapOperation instance, @Coerce char c, @Coerce short i, int j, Operation<Void> original) {
    }

    @WrapOperation(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test3(@Coerce Object instance, @Coerce Object x, @Coerce char y, Operation<?> original) {
    }

    @WrapOperation(method = "caller", at = @At(value = "FIELD", target = "test"))
    private void test4(MixedInWrapOperation instance, String value, Operation<Void> original) {
    }

    @WrapOperation(
            method = "caller",
            at = {@At(value = "INVOKE", target = "intValue"), @At(value = "FIELD", target = "test2", opcode = 180)}
    )
    private @Coerce char test5(@Coerce Comparable<? extends Comparable<?>> instance, Operation<? extends Serializable> original) {
        return 0;
    }

    @WrapOperation(method = "caller", at = @At(value = "CONSTANT", args = "classValue=java/lang/Integer"))
    private boolean test6(Object object, Operation<Boolean> original) {
        return false;
    }

    @Expression("? == 3")
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean test7(int left, int right, Operation<Boolean> original) { // expect-template
        return false;
    }

    @Expression({"? == 3", "? == 4"})
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean test8(int left, int right, Operation<Boolean> original) { // expect-template
        return false;
    }

    @Definition(id = "callee4", method = "callee4")
    @Expression({"? == 3", "callee4(?, ?)"})
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private @Coerce boolean test9(char c, char c1, Operation<? extends Serializable> original) {
        return false;
    }

    @Definition(id = "callee5", method = "callee5")
    @Expression({"? == 3", "callee5(?, ?)"})
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean test10(int i, @Coerce short i1, Operation<Boolean> original) {
        return false;
    }

    @Definition(id = "callee6", method = "callee6")
    @Expression({"? == 3", "callee6(?, ?)"})
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean test11(short i, @Coerce short i1, Operation<Boolean> original) {
        return false;
    }

    @Expression("? == 3")
    @WrapOperation(method = "caller2", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean test12(int a, @Coerce boolean b, Operation<Boolean> original) {
        return b;
    }
}