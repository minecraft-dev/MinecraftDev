package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyReturnValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.io.Serializable;

@Mixin(MixedInModifyReturnValue.class)
class TestMixin {
    @ModifyReturnValue(method = "method1", at = @At("RETURN"))
    private String test1(String original) {
        return original;
    }

    @ModifyReturnValue(method = {"method1", "method2"}, at = @At("RETURN"))
    private @Coerce Serializable test2(@Coerce Serializable original) {
        return original;
    }

    @ModifyReturnValue(method = "method1", at = @At("RETURN"))
    private String test3(@Coerce CharSequence original) {
        return null;
    }

    @ModifyReturnValue(method = {"method2", "method3"}, at = @At("RETURN"))
    private static @Coerce Number test4(@Coerce Number original) {
        return original;
    }

    @ModifyReturnValue(method = {"method4", "method5"}, at = @At("RETURN"))
    private @Coerce char test5(@Coerce char original, String a) {
        return original;
    }

    @ModifyReturnValue(method = {"method4", "method5"}, at = @At("RETURN"))
    private @Coerce char test6(@Coerce char original, String a, @Coerce short b) {
        return original;
    }
}