package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.MinecraftSettings;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import java.util.function.Function;

public class ColorAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!MinecraftSettings.getInstance().isShowChatColorUnderlines()) {
            return;
        }

        final Color color = ColorUtil.findColorFromElement(element, Map.Entry::getValue);
        if (color == null) {
            return;
        }

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
