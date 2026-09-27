package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyVariable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MixedInModifyVariable.class)
public class TestMixin {
    @ModifyVariable(method = "method1", at = @At("RETURN"))
    private String test1(String arg) {
        return arg;
    }

    @ModifyVariable(method = "method1", at = @At("RETURN"))
    private int test2(int local1) {
        return local1;
    }

    @ModifyVariable(method = "method1", at = @At("RETURN"))
    private CharSequence test3(CharSequence local2) {
        return local2;
    }

    @ModifyVariable(method = "method3", at = @At("RETURN"), index = 4)
    private int test4(int local3) {
        return local3;
    }

    @ModifyVariable(method = "method1", at = @At("RETURN"))
    private char test5(char original, String arg) {
        return original;
    }

    @ModifyVariable(method = {"method1", "method3"}, at = @At("RETURN"))
    private String test6(String original) {
        return original;
    }
}