package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInWrapMethod;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(MixedInWrapMethod.class)
class TestMixin {
    @WrapMethod(method = "method1")
    private @Coerce String test1() {
    }

    @WrapMethod(method = "method1")
    private String test2(@Coerce CharSequence argHopefullyPreserved, Operation<?> original) {
    }

    @WrapMethod(method = {"method1", "method3"})
    private void test3() {
    }

    @WrapMethod(method = {"method2", "method4"})
    private void test4() {
    }

    @WrapMethod(method = {"method5", "method6"})
    private void test5() {
    }
}
