package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyArg;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MixedInModifyArg.class)
public class TestMixin {
    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private String test1(String x) {
        return x;
    }

    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private String test2(String x) {
        return x;
    }

    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private int test3(int y) {
        return y;
    }

    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"), index = 1)
    private int test4(int y) {
        return y;
    }

    @ModifyArg(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private String test5(String x, int y) {
        return x;
    }

    @ModifyArg(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee1")}
    )
    private int test6(int y) {
        return y;
    }
}