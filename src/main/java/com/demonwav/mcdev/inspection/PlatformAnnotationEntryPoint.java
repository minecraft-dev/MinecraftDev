/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.inspection;

import com.google.common.collect.ImmutableList;
import com.intellij.codeInspection.reference.EntryPoint;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlatformAnnotationEntryPoint extends EntryPoint {
    @NotNull
    @Override
    public String getDisplayName() {
        return "Minecraft Entry Point";
    }

    @Override
    public boolean isEntryPoint(@NotNull RefElement refElement, @NotNull PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean isEntryPoint(@NotNull PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean isSelected() {
        return false;
    }

    @Override
    public void setSelected(boolean selected) {

    }

    @Nullable
    @Override
    public String[] getIgnoreAnnotations() {
        return ImmutableList.of("org.spongepowered.api.event.Listener", "org.bukkit.event.EventHandler").toArray(new String[2]);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {

    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {

    }
}
