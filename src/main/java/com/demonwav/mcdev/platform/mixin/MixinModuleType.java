package com.demonwav.mcdev.platform.mixin;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

public class MixinModuleType extends AbstractModuleType<MixinModule> {

    public static final String ID = "MIXIN_MODULE_TYPE";
    private static final MixinModuleType instance = new MixinModuleType();

    private static final ImmutableList<String> ANNOTATIONS = ImmutableList.<String>builder()
            .add(Annotations.DEBUG)
            .add(Annotations.FINAL)
            .add(Annotations.IMPLEMENTS)
            .add(Annotations.INTERFACE)
            .add(Annotations.INTRINSIC)
            .add(Annotations.MIXIN)
            .add(Annotations.MUTABLE)
            .add(Annotations.OVERWRITE)
            .add(Annotations.SHADOW)
            .add(Annotations.SOFT_OVERRIDE)
            .add(Annotations.UNIQUE)
            .add(Annotations.INJECT)
            .add(Annotations.MODIFY_ARG)
            .add(Annotations.MODIFY_CONSTANT)
            .add(Annotations.MODIFY_VARIABLE)
            .add(Annotations.REDIRECT)
            .add(Annotations.SURROGATE)
            .build();

    private MixinModuleType() {
        super("org.spongepowered", "mixin");
    }

    @NotNull
    public static MixinModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.MIXIN;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean hasIcon() {
        return false;
    }

    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ANNOTATIONS;
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of();
    }

    @NotNull
    @Override
    public MixinModule generateModule(Module module) {
        return new MixinModule(module);
    }
}
