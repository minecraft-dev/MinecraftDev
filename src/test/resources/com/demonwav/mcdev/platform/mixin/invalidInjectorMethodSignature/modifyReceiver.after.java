package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(MixedInModifyReceiver.class)
class TestMixin {
    @ModifyReceiver(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private MixedInModifyReceiver test1(MixedInModifyReceiver instance, String x, int y) {
        return instance;
    }

    @ModifyReceiver(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee3")}
    )
    private static MixedInModifyReceiver test2(MixedInModifyReceiver instance, @Coerce char c, @Coerce short i, int j) {
        return instance;
    }

    @ModifyReceiver(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private MixedInModifyReceiver test3(@Coerce Object instance, @Coerce Object x, @Coerce char y) {
        return null;
    }

    @ModifyReceiver(method = "caller", at = @At(value = "FIELD", target = "test"))
    private MixedInModifyReceiver test4(MixedInModifyReceiver instance, String value) {
        return instance;
    }

    @ModifyReceiver(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee1"), @At(value = "FIELD", target = "test")}
    )
    private static MixedInModifyReceiver test5(MixedInModifyReceiver instance, String s, @Coerce char c) {
        return instance;
    }

    @ModifyReceiver(method = "caller", at = @At(value = "FIELD", target = "test2"))
    private MixedInModifyReceiver test6(MixedInModifyReceiver instance, char c) {
        return instance;
    }

    @ModifyReceiver(
            method = "caller",
            at = {@At(value = "INVOKE", target = "longValue"), @At(value = "FIELD", target = "test2", opcode = 180)}
    )
    private @Coerce Comparable<? extends Comparable<?>> test7(@Coerce Comparable<? extends Comparable<?>> instance) {
        return instance;
    }
}