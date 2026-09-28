package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MixedInModifyReceiver.class)
class TestMixin {
    @ModifyReceiver(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test1() {
    }

    @ModifyReceiver(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee3")}
    )
    private static void test2() {
    }

    @ModifyReceiver(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test3(@Coerce Object instance, @Coerce Object x, @Coerce char y) {
    }

    @ModifyReceiver(method = "caller", at = @At(value = "FIELD", target = "test"))
    private void test4() {
    }

    @ModifyReceiver(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee1"), @At(value = "FIELD", target = "test")}
    )
    private static void test5() {
    }

    @ModifyReceiver(method = "caller", at = @At(value = "FIELD", target = "test2"))
    private void test6() {
    }

    @ModifyReceiver(
            method = "caller",
            at = {@At(value = "INVOKE", target = "longValue"), @At(value = "FIELD", target = "test2", opcode = 180)}
    )
    private void test7() {
    }
}
