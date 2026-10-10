package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInComplex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MixedInComplex.class)
class TestMixin {
    @Inject(method = {"method1", "method2"}, at = @At("RETURN"))
    private void inject1() {
    }

    @Inject(method = {"method2", "method3"}, at = @At("RETURN"))
    private void inject2() {
    }

    @Inject(method = {"method1", "method3"}, at = @At("RETURN"))
    private void inject3() {
    }

    @Inject(method = {"method3", "method4"}, at = @At("RETURN"))
    private void inject4() {
    }

    @Inject(method = {"method1", "method2"}, at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void inject5() {
    }

    @Inject(method = {"method1", "method5"}, at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void inject6() {
    }

    @Inject(method = {"method5", "method6"}, at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void inject7() {
    }

    @Inject(method = {"method1", "method3"}, at = @At("RETURN"))
    private String inject8(@Coerce Object arg, @Coerce CallbackInfo ci) {
    }

    @Inject(method = {"method7", "method8"}, at = @At("HEAD"))
    private void inject9(@Coerce int a, CallbackInfo ci) {
    }
}
