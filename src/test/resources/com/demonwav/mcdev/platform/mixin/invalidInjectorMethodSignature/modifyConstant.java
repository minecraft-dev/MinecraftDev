package test;

import com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix.MixedInModifyConstant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.io.Serializable;

@Mixin(MixedInModifyConstant.class)
class TestMixin {
    @ModifyConstant(method = "method1", constant = @Constant(intValue = 42))
    private void test1() {
    }

    @ModifyConstant(method = "method1", constant = @Constant(intValue = 42))
    private void test2(@Coerce short shouldBePreserved) {
    }

    @ModifyConstant(method = "method1", constant = @Constant(classValue = Long.class))
    private static void test3() {
    }

    @ModifyConstant(method = "method1", constant = @Constant(classValue = Integer.class))
    private void test4() {
    }

    @ModifyConstant(
            method = "method1",
            constant = {@Constant(classValue = Long.class), @Constant(stringValue = "hello")}
    )
    private void test5() {
    }

    @ModifyConstant(
            method = "method1",
            constant = {@Constant(classValue = Integer.class), @Constant(classValue = Long.class)}
    )
    private static void test6() {
    }

    @ModifyConstant(method = "method1", constant = @Constant(classValue = Integer.class))
    private void test7(Object obj, Class<?> clazz, @Coerce Serializable shouldBePreserved) {
    }
}
