package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.util.Function;
import com.intellij.util.FunctionUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

public class ColorLineMarkerProvider extends LineMarkerProviderDescriptor {

    @Nullable
    @Override
    public String getName() {
        return null;
    }

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (!(element instanceof PsiReferenceExpression)) {
            return null;
        }
        
        final Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return null;
        }

        MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule == null) {
            return null;
        }

        PsiReferenceExpression expression = (PsiReferenceExpression) element;
        PsiType type = expression.getType();
        if (type == null) {
            return null;
        }

        if (type.getCanonicalText().startsWith("org.bukkit.ChatColor")) {
            System.out.println("stop");
        }

        for (AbstractModuleType<?> abstractModuleType : minecraftModule.getTypes()) {
            Map<String, Color> map = abstractModuleType.getClassToColorMappings();
            for (Map.Entry<String, Color> entry : map.entrySet()) {
                // This is such a hack
                // Okay, type will be the fully-qualified class, but it will exclude the actual enum
                // the expression will be the non-fully-qualified class with the enum
                // So we combine those checks and get this
                if (entry.getKey().startsWith(type.getCanonicalText()) &&
                        entry.getKey().endsWith(expression.getCanonicalText())) {
                    return new LineMarkerInfo<>(
                            element,
                            element.getTextRange(),
                            getIconFromColor(entry.getValue()),
                            Pass.UPDATE_ALL,
                            FunctionUtil.<Object, String>nullConstant(),
                            null,
                            GutterIconRenderer.Alignment.CENTER
                    );
                }
            }
        }
        return null;
    }

    @NotNull
    private Icon getIconFromColor(@NotNull Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(color);
                g.fillRect(x, y, getIconWidth(), getIconHeight());
            }

            @Override
            @Contract(pure = true)
            public int getIconWidth() {
                return 12;
            }

            @Override
            @Contract(pure = true)
            public int getIconHeight() {
                return 12;
            }
        };
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    }
}
