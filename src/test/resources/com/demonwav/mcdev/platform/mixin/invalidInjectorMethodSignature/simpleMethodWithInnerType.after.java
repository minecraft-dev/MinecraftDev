package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInOuter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MixedInOuter.class)
public class TestMixin {

    @Inject(method = "methodWithInnerType", at = @At("RETURN"))
    private void injectCtor(MixedInOuter.MixedInInner param, CallbackInfoReturnable<MixedInOuter.MixedInInner> cir) {
    }
}
