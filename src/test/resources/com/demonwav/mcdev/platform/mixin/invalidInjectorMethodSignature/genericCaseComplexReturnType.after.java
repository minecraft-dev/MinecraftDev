package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInGeneric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(MixedInGeneric.class)
public class TestMixin {

    @Inject(method = "returnComplex", at = @At("RETURN"))
    private void injectGeneric(CallbackInfoReturnable<Map.Entry<String, List<Map.Entry<String, Map<Integer, int[]>>>[]>> cir) {
    }
}
