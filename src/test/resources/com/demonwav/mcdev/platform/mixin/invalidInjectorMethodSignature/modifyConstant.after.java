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
    private int test1(int constant) {
        return constant;
    }

    @ModifyConstant(method = "method1", constant = @Constant(intValue = 42))
    private int test2(@Coerce short shouldBePreserved) {
        return 0;
    }

    @ModifyConstant(method = "method1", constant = @Constant(classValue = Long.class))
    private static Class<?> test3(Class<?> constant) {
        return constant;
    }

    @ModifyConstant(method = "method1", constant = @Constant(classValue = Integer.class))
    private boolean test4(Object instance, Class<?> type) {
        return false;
    }

    @ModifyConstant(
            method = "method1",
            constant = {@Constant(classValue = Long.class), @Constant(stringValue = "hello")}
    )
    private @Coerce Serializable test5(@Coerce Serializable constant) {
        return constant;
    }

    @ModifyConstant(
            method = "method1",
            constant = {@Constant(classValue = Integer.class), @Constant(classValue = Long.class)}
    )
    private static Class<?> test6(@Coerce Object o, Class<?> aClass) {
        return aClass;
    }

    @ModifyConstant(method = "method1", constant = @Constant(classValue = Integer.class))
    private boolean test7(Object obj, Class<?> clazz, @Coerce Serializable shouldBePreserved) {
        return false;
    }
}