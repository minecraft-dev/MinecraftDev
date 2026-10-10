package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInOuter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(MixedInOuter.class)
public class TestMixin {

    @Inject(method = "methodWithInnerType", at = @At("RETURN"))
    private void injectCtor(<caret>) {
    }
}
