/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class ShadowedMembers {

    public static final ShadowedMembers EMPTY = new ShadowedMembers();

    @NotNull
    private final List<PsiElement> targets = Lists.newArrayList();
    @NotNull
    private final List<ShadowError> errors = Lists.newArrayList();

    private ShadowedMembers() {
    }

    public static ShadowedMembers create() {
        return new ShadowedMembers();
    }

    @NotNull
    public List<PsiElement> getTargets() {
        return targets;
    }

    public ShadowedMembers addTarget(@NotNull PsiElement target) {
        this.targets.add(target);
        return this;
    }

    public ShadowedMembers addTargets(@NotNull Collection<? extends PsiElement> targets) {
        this.targets.addAll(targets);
        return this;
    }

    @NotNull
    public List<ShadowError> getErrors() {
        return errors;
    }

    public ShadowedMembers addError(@NotNull ShadowError error) {
        this.errors.add(error);
        return this;
    }

    public ShadowedMembers addErrors(@NotNull Collection<ShadowError> errors) {
        this.errors.addAll(errors);
        return this;
    }
}
