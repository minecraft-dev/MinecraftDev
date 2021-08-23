package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.GenericOneParam;
import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.GenericTwoParams;
import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInGeneric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(MixedInGeneric.class)
public class TestMixin {

    @Inject(method = "genericMethod", at = @At("RETURN"))
    private void injectGeneric(String noGenerics, GenericOneParam<String> oneParam, GenericTwoParams<String, Integer> twoParams, GenericOneParam<GenericOneParam<String>> nestedParam, Map<String, List<Map.Entry<String, Map<Integer, int[]>>>[]> pleaseJava, CallbackInfoReturnable<GenericOneParam<String>> cir) {
    }
}
