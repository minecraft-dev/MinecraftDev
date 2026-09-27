package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyVariable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MixedInModifyVariable.class)
public class TestMixin {
    @ModifyVariable(method = "method1", at = @At("RETURN"))
    private void test1() {
    }

    @ModifyVariable(method = "method1", at = @At("RETURN"))
    private int test2() {
    }

    @ModifyVariable(method = "method1", at = @At("RETURN"))
    private CharSequence test3() {
    }

    @ModifyVariable(method = "method3", at = @At("RETURN"), index = 4)
    private String test4() {
    }

    @ModifyVariable(method = "method1", at = @At("RETURN"))
    private void test5(char original, String arg) {
    }

    @ModifyVariable(method = {"method1", "method3"}, at = @At("RETURN"))
    private void test6() {
    }
}
