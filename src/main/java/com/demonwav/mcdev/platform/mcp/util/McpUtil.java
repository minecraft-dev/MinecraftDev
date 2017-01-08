/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.util;

import com.google.common.collect.Lists;
import com.intellij.navigation.AnonymousElementProvider;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class McpUtil {
    private McpUtil() {
    }

    /**
     * Given a Mixin target class string (the string given to the {@code targets} Mixin annotation attribute), find, if possible,
     * the corresponding class.
     *
     * @param s The String to check.
     * @return The corresponding class for the given String, or null if not found.
     */
    @Nullable
    @Contract(value = "null, _ -> null", pure = true)
    public static PsiClass getClassFromString(@Nullable String s, @NotNull Project project) {
        s = normalizeClassString(s);
        if (s == null) {
            return null;
        }

        final String replaced = s.replaceAll("/", ".");
        String text = replaced;
        if (text.contains("$")) {
            text = text.substring(0, text.indexOf('$'));
        }

        final PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(text, GlobalSearchScope.allScope(project));
        if (!replaced.contains("$")) {
            return psiClass;
        }

        if (psiClass == null) {
            return null;
        }

        // Handle anonymous and inner classes
        final String[] classes = replaced.substring(replaced.indexOf('$')).split("\\$");
        List<Object> indexes = Lists.newArrayList();
        for (String cls : classes) {
            if (cls.isEmpty()) {
                continue;
            }

            try {
                indexes.add(Integer.parseInt(cls) - 1);
            } catch (Exception e) {
                indexes.add(cls);
            }
        }

        PsiElement current = psiClass;
        for (Object index : indexes) {
            if (index instanceof Integer) {
                PsiElement[] anonymousClasses = null;
                for (AnonymousElementProvider provider : Extensions.getExtensions(AnonymousElementProvider.EP_NAME)) {
                    anonymousClasses = provider.getAnonymousElements(psiClass);
                    if (anonymousClasses.length > 0) {
                        break;
                    }
                }
                if (anonymousClasses == null) {
                    return psiClass;
                }

                if ((((Integer) index) >= 0) && (((Integer) index) < anonymousClasses.length)) {
                    current = anonymousClasses[((Integer) index)];
                } else {
                    return (PsiClass) current;
                }
            } else {
                final PsiClass newClass = ((PsiClass) current).findInnerClassByName((String) index, false);
                if (newClass == null) {
                    return ((PsiClass) current);
                }
                current = newClass;
            }
        }

        return (PsiClass) current;
    }

    @Contract(value = "null -> null", pure = true)
    public static String normalizeClassString(@Nullable String s) {
        if (s == null) {
            return null;
        }

        while (s.startsWith("[")) {
            s = s.substring(1);
        }

        if (s.startsWith("L")) {
            s = s.substring(1);
        }
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    @Contract(value = "null -> null", pure = true)
    public static String replaceDotWithSlash(@Nullable String s) {
        if (s == null) {
            return null;
        }

        return s.replaceAll("\\.", "/");
    }

    @Contract(value = "null -> null", pure = true)
    public static String replaceSlashWithDot(@Nullable String s) {
        if (s == null) {
            return null;
        }

        return s.replaceAll("/", ".");
    }

    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static PsiType getPrimitiveType(String s) {
        if (s == null) {
            return null;
        }

        switch (s) {
            case "B":
                return PsiType.BYTE;
            case "C":
                return PsiType.CHAR;
            case "D":
                return PsiType.DOUBLE;
            case "F":
                return PsiType.FLOAT;
            case "I":
                return PsiType.INT;
            case "J":
                return PsiType.LONG;
            case "S":
                return PsiType.SHORT;
            case "Z":
                return PsiType.BOOLEAN;
            default:
                return null;
        }
    }

    @Nullable
    @Contract(value = "null -> null", pure = true)
    public static String getStringFromType(@Nullable PsiType type) {
        if (type == null) {
            return null;
        }

        if (type == PsiType.BYTE) {
            return "B";
        } else if (type == PsiType.CHAR) {
            return "C";
        } else if (type == PsiType.DOUBLE) {
            return "D";
        } else if (type == PsiType.FLOAT) {
            return "F";
        } else if (type == PsiType.INT) {
            return "I";
        } else if (type == PsiType.LONG) {
            return "J";
        } else if (type == PsiType.SHORT) {
            return "S";
        } else if (type == PsiType.BOOLEAN) {
            return "Z";
        } else if (type == PsiType.VOID) {
            return "V";
        } else {
            return "L" + type.getCanonicalText(false).replaceAll("\\.", "/") + ";";
        }
    }
}
