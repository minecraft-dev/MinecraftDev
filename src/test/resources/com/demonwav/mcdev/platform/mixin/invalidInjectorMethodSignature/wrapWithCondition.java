package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInWrapWithCondition;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MixedInWrapWithCondition.class)
class TestMixin {
    @WrapWithCondition(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test1() {
    }

    @WrapWithCondition(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee3")}
    )
    private static void test2() {
    }

    @WrapWithCondition(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test3(@Coerce Object instance, @Coerce Object x, @Coerce char y) {
    }

    @WrapWithCondition(method = "caller", at = @At(value = "FIELD", target = "test"))
    private void test4() {
    }

    @WrapWithCondition(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee1"), @At(value = "FIELD", target = "test")}
    )
    private static void test5() {
    }

    @WrapWithCondition(method = "caller", at = @At(value = "FIELD", target = "test2", opcode = 181))
    private void test6() {
    }
}
