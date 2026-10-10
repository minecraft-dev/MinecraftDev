package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyReturnValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(MixedInModifyReturnValue.class)
class TestMixin {
    @ModifyReturnValue(method = "method1", at = @At("RETURN"))
    private void test1() {
    }

    @ModifyReturnValue(method = {"method1", "method2"}, at = @At("RETURN"))
    private void test2() {
    }

    @ModifyReturnValue(method = "method1", at = @At("RETURN"))
    private void test3(@Coerce CharSequence original) {
    }

    @ModifyReturnValue(method = {"method2", "method3"}, at = @At("RETURN"))
    private static void test4() {
    }

    @ModifyReturnValue(method = {"method4", "method5"}, at = @At("RETURN"))
    private void test5(@Coerce char original, String a) {
    }

    @ModifyReturnValue(method = {"method4", "method5"}, at = @At("RETURN"))
    private void test6(@Coerce char original, String a, @Coerce short b) {
    }
}
