package com.demonwav.mcdev.insight;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Map;
import java.util.function.Function;

public class ColorUtil {

    public static <T> T findColorFromElement(@NotNull PsiElement element, @NotNull Function<Map.Entry<String, Color>, T> function) {
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

        for (AbstractModuleType<?> abstractModuleType : minecraftModule.getTypes()) {
            Map<String, Color> map = abstractModuleType.getClassToColorMappings();
            for (Map.Entry<String, Color> entry : map.entrySet()) {
                // This is such a hack
                // Okay, type will be the fully-qualified class, but it will exclude the actual enum
                // the expression will be the non-fully-qualified class with the enum
                // So we combine those checks and get this
                if (entry.getKey().startsWith(type.getCanonicalText()) &&
                        entry.getKey().endsWith(expression.getCanonicalText())) {
                    return function.apply(entry);
                }
            }
        }
        return null;
    }
}
