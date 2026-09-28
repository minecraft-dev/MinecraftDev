package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyArg;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MixedInModifyArg.class)
class TestMixin {
    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test1() {
    }

    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private String test2() {
    }

    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private int test3() {
    }

    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"), index = 1)
    private String test4() {
    }

    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test5(String x, int y) {
    }

    @ModifyArg(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee1")}
    )
    private void test6() {
    }
}
