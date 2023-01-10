package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInSimple;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MixedInSimple.class)
public class TestMixin {

    @Inject(method = "simpleMethod", at = @At("RETURN"))
    private void injectCtor(String string, CallbackInfo ci, @Local String str<caret>) {
    }
}
