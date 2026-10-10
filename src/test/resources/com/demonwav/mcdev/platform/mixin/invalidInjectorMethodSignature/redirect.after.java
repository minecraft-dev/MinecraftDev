package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.Serializable;

@Mixin(MixedInRedirect.class)
class TestMixin {
    @Redirect(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test1(MixedInRedirect instance, String x, int y) {
    }

    @Redirect(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee2"), @At(value = "INVOKE", target = "callee3")}
    )
    private static void test2(MixedInRedirect instance, @Coerce char c, @Coerce short i, int j) {
    }

    @Redirect(method = "caller", at = @At(value = "INVOKE", target = "callee1"))
    private void test3(@Coerce Object instance, @Coerce Object x, @Coerce char y) {
    }

    @Redirect(method = "caller", at = @At(value = "FIELD", target = "test"))
    private void test4(MixedInRedirect instance, String value) {
    }

    @Redirect(
            method = "caller",
            at = {@At(value = "INVOKE", target = "callee1"), @At(value = "FIELD", target = "test")}
    )
    private static void test5(MixedInRedirect instance, String s, @Coerce char c) {
    }

    @Redirect(
            method = "caller",
            at = {@At(value = "INVOKE", target = "intValue"), @At(value = "FIELD", target = "test2", opcode = 180)}
    )
    private @Coerce char test6(@Coerce Comparable<? extends Comparable<?>> instance) {
        return 0;
    }

    @Redirect(method = "caller", at = @At(value = "CONSTANT", args = "classValue=java/lang/Integer"))
    private boolean test7(Object instance, Class<?> type) {
        return false;
    }

    @Redirect(
            method = "caller",
            at = {@At(value = "CONSTANT", args = "classValue=java/lang/Integer"), @At(value = "INVOKE", target = "callee4")}
    )
    private Class<?> test8(@Coerce Object instance, Class<?> aClass) {
        return aClass;
    }

    @Redirect(method = "caller2", at = @At(value = "CONSTANT", args = "classValue=java/lang/Integer"))
    private boolean test7(Object obj, Class<?> clazz, @Coerce Serializable shouldBePreserved) {
        return false;
    }
}