package com.demonwav.mcdev.platform.mixin;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class MixinModuleType extends AbstractModuleType<MixinModule> {
    public static final String ID = "MIXIN_MODULE_TYPE";
    private static final MixinModuleType instance = new MixinModuleType();

    public static MixinModuleType getInstance() {
        return instance;
    }

    private MixinModuleType() {
        super("org.spongepowered", "mixin");
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

    private final ImmutableList<String> annotations = ImmutableList.<String>builder()
            .add("org.spongepowered.asm.mixin.Debug")
            .add("org.spongepowered.asm.mixin.Final")
            .add("org.spongepowered.asm.mixin.Implements")
            .add("org.spongepowered.asm.mixin.Interface")
            .add("org.spongepowered.asm.mixin.Intrinsic")
            .add("org.spongepowered.asm.mixin.Mixin")
            .add("org.spongepowered.asm.mixin.Mutable")
            .add("org.spongepowered.asm.mixin.Overwrite")
            .add("org.spongepowered.asm.mixin.Shadow")
            .add("org.spongepowered.asm.mixin.SoftOverride")
            .add("org.spongepowered.asm.mixin.Unique")
            .add("org.spongepowered.asm.mixin.injection.Inject")
            .add("org.spongepowered.asm.mixin.injection.ModifyArg")
            .add("org.spongepowered.asm.mixin.injection.ModifyConstant")
            .add("org.spongepowered.asm.mixin.injection.ModifyVariable")
            .add("org.spongepowered.asm.mixin.injection.Redirect")
            .add("org.spongepowered.asm.mixin.injection.Surrogate")
            .build();

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return this.annotations;
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
