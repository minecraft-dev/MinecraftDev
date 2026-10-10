package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInWrapWithCondition;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(MixedInWrapWithCondition.class)
class TestMixin {
    @WrapWithCondition(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private boolean test1(MixedInWrapWithCondition instance, String x, int y) {
        return false;
    }

    @WrapWithCondition(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee3")}
    )
    private static boolean test2(MixedInWrapWithCondition instance, @Coerce char c, @Coerce short i, int j) {
        return false;
    }

    @WrapWithCondition(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private boolean test3(@Coerce Object instance, @Coerce Object x, @Coerce char y) {
        return false;
    }

    @WrapWithCondition(method = "caller", at = @At(value = "FIELD", target = "test"))
    private boolean test4(MixedInWrapWithCondition instance, String value) {
        return false;
    }

    @WrapWithCondition(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee1"), @At(value = "FIELD", target = "test")}
    )
    private static boolean test5(MixedInWrapWithCondition instance, String s, @Coerce char c) {
        return false;
    }

    @WrapWithCondition(method = "caller", at = @At(value = "FIELD", target = "test2", opcode = 181))
    private boolean test6(MixedInWrapWithCondition instance, char value) {
        return false;
    }
}