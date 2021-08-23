package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInGeneric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(MixedInGeneric.class)
public class TestMixin {

    @Inject(method = "genericMethod", at = @At("RETURN"))
    private void injectGeneric(<caret>) {
    }
}
