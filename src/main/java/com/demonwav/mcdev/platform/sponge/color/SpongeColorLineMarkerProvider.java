package com.demonwav.mcdev.platform.sponge.color;

import com.demonwav.mcdev.insight.ColorLineMarkerProvider;
import com.demonwav.mcdev.insight.ColorUtil;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.NavigateAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.ColorChooser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

public class SpongeColorLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        Pair<Color, PsiElement> pair = SpongeColorUtil.findColorFromElement(element);
        if (pair == null) {
            return null;
        }

        SpongeColorInfo info = new SpongeColorInfo(element, pair.first, pair.second);
        NavigateAction.setNavigateAction(info, "Change color", null);

        return info;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    }

    private static class SpongeColorInfo extends ColorLineMarkerProvider.ColorInfo {

        public SpongeColorInfo(@NotNull final PsiElement element, @NotNull Color color, @NotNull PsiElement workElement) {
            super(
                element,
                color,
                (event, psiElement) -> {
                    if (!element.isWritable()) {
                        return;
                    }

                    final Editor editor = PsiUtilBase.findEditor(element);
                    if (editor == null) {
                        return;
                    }

                    final Color c = ColorChooser.chooseColor(editor.getComponent(), "Choose Color", color, false);
                    if (c != null) {
                        if (workElement instanceof PsiLiteralExpression) {
                            ColorUtil.setColorTo((PsiLiteralExpression) workElement, c.getRGB() & 0xFFFFFF);
                        } else if (workElement instanceof PsiExpressionList) {
                            ColorUtil.setColorTo((PsiExpressionList) workElement, c.getRed(), c.getGreen(), c.getBlue());
                        } else if (workElement instanceof PsiNewExpression) {
                            PsiExpressionList expressionList = (PsiExpressionList) workElement.getNode()
                                .findChildByType(JavaElementType.EXPRESSION_LIST);

                            if (expressionList != null) {
                                ColorUtil.setColorTo(expressionList, c.getRed(), c.getGreen(), c.getBlue());
                            }
                        }
                    }
                }
            );
        }
    }
}
