package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.Serializable;

@Mixin(MixedInRedirect.class)
class TestMixin {
    @Redirect(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test1() {
    }

    @Redirect(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee3")}
    )
    private static void test2() {
    }

    @Redirect(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private String test3(@Coerce Object instance, @Coerce Object x, @Coerce char y) {
    }

    @Redirect(method = "caller", at = @At(value = "FIELD", target = "test"))
    private void test4() {
    }

    @Redirect(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee1"), @At(value = "FIELD", target = "test")}
    )
    private static void test5() {
    }

    @Redirect(
            method = "caller",
            at = {@At(value = "INVOKE", target = "intValue"), @At(value = "FIELD", target = "test2", opcode = 180)}
    )
    private void test6() {
    }

    @Redirect(method = "caller", at = @At(value = "CONSTANT", args = "classValue=java/lang/Integer"))
    private void test7() {
    }

    @Redirect(
            method = "caller",
            at = {@At(value = "CONSTANT", args = "classValue=java/lang/Integer"), @At(value = "INVOKE", target = "callee4")}
    )
    private void test8() {
    }

    @Redirect(method = "caller2", at = @At(value = "CONSTANT", args = "classValue=java/lang/Integer"))
    private void test7(Object obj, Class<?> clazz, @Coerce Serializable shouldBePreserved) {
    }
}
