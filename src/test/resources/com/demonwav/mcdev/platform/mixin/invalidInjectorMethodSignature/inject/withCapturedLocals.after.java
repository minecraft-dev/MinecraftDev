package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInSimple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MixedInSimple.class)
public class TestMixin {

    @Inject(method = "simpleMethod", at = @At("RETURN"))
    private void injectCtor(String string, int i, CallbackInfo ci, String local1, float local2, int local3<caret>) {
    }
}
