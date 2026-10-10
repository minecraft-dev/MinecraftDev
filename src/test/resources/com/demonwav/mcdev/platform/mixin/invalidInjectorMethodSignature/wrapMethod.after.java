package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInWrapMethod;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.Collection;

@Mixin(MixedInWrapMethod.class)
class TestMixin {
    @WrapMethod(method = "method1")
    private void test1(String arg, Operation<Void> original) {
    }

    @WrapMethod(method = "method1")
    private void test2(@Coerce CharSequence argHopefullyPreserved, Operation<?> original) {
    }

    @WrapMethod(method = {"method1", "method3"})
    private void test3(@Coerce CharSequence arg, Operation<Void> original) {
    }

    @WrapMethod(method = {"method2", "method4"})
    private @Coerce Number test4(Operation<? extends Number> original) {
        return null;
    }

    @WrapMethod(method = {"method5", "method6"})
    private @Coerce Number test5(@Coerce Collection<? extends Number> list, @Coerce char c, Operation<? extends Number> original) {
        return null;
    }
}