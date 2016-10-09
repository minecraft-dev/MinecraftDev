/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.sponge.color;

import com.demonwav.mcdev.MinecraftSettings;
import com.demonwav.mcdev.insight.ColorAnnotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public class SpongeColorAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!MinecraftSettings.getInstance().isShowChatColorUnderlines()) {
            return;
        }

        final Pair<Color, PsiElement> pair = SpongeColorUtil.findColorFromElement(element);
        if (pair == null) {
            return;
        }

        ColorAnnotator.setColorAnnotator(pair.first, element, holder);
    }
}
