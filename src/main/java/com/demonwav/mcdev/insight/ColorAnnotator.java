/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.MinecraftSettings;

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Font;

public class ColorAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!MinecraftSettings.getInstance().isShowChatColorUnderlines()) {
            return;
        }

        final Color color = ColorUtil.findColorFromElement(element, (map, chosenEntry) -> chosenEntry.getValue());
        if (color == null) {
            return;
        }

        setColorAnnotator(color, element, holder);
    }

    public static void setColorAnnotator(@NotNull Color color, @NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        final TextAttributesKey key = TextAttributesKey.createTextAttributesKey("MC_COLOR_" + color.toString(), new TextAttributes(
            null,
            null,
            color,
            MinecraftSettings.getInstance().getUnderlineType().getEffectType(),
            Font.PLAIN
        ));
        // We need to reset it even though we passed it in the create method, since the TextAttributesKey's are cached, so if this changes
        // then the cached version of it still wont. We set it here to make sure it's always set properly
        key.getDefaultAttributes().setEffectType(MinecraftSettings.getInstance().getUnderlineType().getEffectType());
        final Annotation annotation = new Annotation(
            element.getTextRange().getStartOffset(),
            element.getTextRange().getEndOffset(),
            HighlightSeverity.INFORMATION,
            null,
            null
        );
        annotation.setTextAttributes(key);
        ((AnnotationHolderImpl) holder).add(annotation);
    }
}
