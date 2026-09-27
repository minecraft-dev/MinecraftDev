package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInComplex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.Serializable;

@Mixin(MixedInComplex.class)
public class TestMixin {
    @Inject(method = {"method1", "method2"}, at = @At("RETURN"))
    private void inject1(CallbackInfo ci) {
    }

    @Inject(method = {"method2", "method3"}, at = @At("RETURN"))
    private void inject2(@Coerce CallbackInfo ci) {
    }

    @Inject(method = {"method1", "method3"}, at = @At("RETURN"))
    private void inject3(@Coerce Serializable arg, @Coerce CallbackInfo ci) {
    }

    @Inject(method = {"method3", "method4"}, at = @At("RETURN"))
    private void inject4(CallbackInfoReturnable<? extends Serializable> cir) {
    }

    @Inject(method = {"method1", "method2"}, at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void inject5(CallbackInfo ci) {
    }

    @Inject(method = {"method1", "method5"}, at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void inject6(@Coerce CharSequence arg, CallbackInfo ci, int local1, @Coerce CharSequence local2, @Coerce char local3) {
    }

    @Inject(method = {"method1", "method3"}, at = @At("RETURN"))
    private void inject7(@Coerce Object arg, @Coerce CallbackInfo ci) {
    }
}