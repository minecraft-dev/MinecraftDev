/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.srg;

import com.demonwav.mcdev.platform.mcp.util.McpUtil;
import com.demonwav.mcdev.util.McPsiUtil;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An immutable instance of the SrgMap API. This can be used to find mappings between
 */
public final class SrgMap {
    private final Set<File> mappingFiles;

    // Key: MCP
    // Value: SRG
    private final BiMap<String, String> classMap = HashBiMap.create();
    private final BiMap<String, String> fieldMap = HashBiMap.create();
    private final BiMap<String, String> methodMap = HashBiMap.create();

    SrgMap(@NotNull Set<File> mappingFiles) throws IOException {
        this.mappingFiles = mappingFiles;

        // currently only read mcpToSrg
        for (File mappingFile : mappingFiles) {
            if (mappingFile.getName().equals("mcp-srg.srg")) {
                final List<String> lines = Files.readAllLines(mappingFile.toPath());
                for (String line : lines) {
                    final String[] split = line.split("\\s+");
                    switch (split[0]) {
                        case "FD:":
                            // field
                            fieldMap.put(split[1], split[2]);
                            break;
                        case "MD:":
                            // method
                            methodMap.put(split[1] + split[2], split[3] + split[4]);
                            break;
                        default:
                            classMap.put(split[1], split[2]);
                            break;
                    }
                }
                break;
            }
        }
    }

    // Mapping methods
    @Nullable
    @Contract("null -> null")
    public String findClassMcpToSrg(@Nullable String className) {
        if (className == null) {
            return null;
        }
        return classMap.get(className);
    }

    @Nullable
    @Contract("null -> null")
    public String findClassSrgToMcp(@Nullable String className) {
        if (className == null) {
            return null;
        }
        final BiMap<String, String> inverse = classMap.inverse();
        return inverse.get(className);
    }

    @Nullable
    @Contract("null -> null")
    public String findFieldMcpToSrg(@Nullable String field) {
        if (field == null) {
            return null;
        }
        return fieldMap.get(field);
    }

    @Nullable
    @Contract("null -> null")
    public String findFieldSrgToMcp(@Nullable String field) {
        if (field == null) {
            return null;
        }
        final BiMap<String , String> inverse = fieldMap.inverse();
        return inverse.get(field);
    }

    @Nullable
    @Contract("null -> null")
    public String findMethodMcpToSrg(@Nullable String method) {
        if (method == null) {
            return null;
        }
        return methodMap.get(method);
    }

    @Nullable
    @Contract("null -> null")
    public String findMethodSrgToMcp(@Nullable String method) {
        if (method == null) {
            return null;
        }
        final BiMap<String, String> inverse = methodMap.inverse();
        return inverse.get(method);
    }

    // Convenience methods
    @Nullable
    @Contract("null -> null")
    public static String toString(@Nullable PsiClass psiClass) {
        final Pair<String, PsiClass> nameOfClass = McPsiUtil.getNameOfClass(psiClass);
        if (nameOfClass == null) {
            return null;
        }

        return McpUtil.replaceDotWithSlash(nameOfClass.getSecond().getQualifiedName()) + nameOfClass.getFirst();
    }

    @Nullable
    @Contract("null -> null")
    public static String toString(@Nullable PsiField field) {
        if (field == null) {
            return null;
        }

        final PsiClass containingClass = field.getContainingClass();
        if (containingClass == null) {
            return null;
        }

        final String classString = toString(containingClass);

        return classString + "/" + field.getName();
    }

    @Nullable
    @Contract("null -> null")
    public static String toString(@Nullable PsiMethod method) {
        if (method == null) {
            return null;
        }

        final PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return null;
        }

        final String classString = toString(containingClass);

        List<String> params = Lists.newArrayList();

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            params.add(McpUtil.getStringFromType(parameter.getType()));
        }

        return classString + "/" + method.getName() + "(" + params.stream().collect(Collectors.joining()) + ")" +
            McpUtil.getStringFromType(method.getReturnType());
    }

    @Nullable
    @Contract("null, _ -> null")
    public static PsiClass fromClassString(@Nullable String string, @NotNull Project project) {
        return McpUtil.getClassFromString(string, project);
    }

    @Nullable
    @Contract("null, _ -> null")
    public static PsiField fromFieldString(@Nullable String string, @NotNull Project project) {
        if (string == null) {
            return null;
        }

        final String className = string.substring(0, string.lastIndexOf("/"));

        final PsiClass classFromString = McpUtil.getClassFromString(className, project);
        if (classFromString == null) {
            return null;
        }

        return classFromString.findFieldByName(string.substring(string.lastIndexOf("/") + 1), false);
    }

    @Nullable
    @Contract("null, _ -> null")
    public static PsiMethod fromMethodString(@Nullable String string, @NotNull Project project) {
        if (string == null) {
            return null;
        }

        final String classAndMethodName = string.substring(0, string.indexOf('('));
        final String className = classAndMethodName.substring(0, classAndMethodName.lastIndexOf('/'));

        final PsiClass classFromString = McpUtil.getClassFromString(className, project);
        if (classFromString == null) {
            return null;
        }

        final PsiMethod[] methodsByName = classFromString.findMethodsByName(classAndMethodName.substring(classAndMethodName.lastIndexOf('/') + 1), false);
        if (methodsByName.length == 0) {
            return null;
        }

        if (methodsByName.length == 1) {
            return methodsByName[0];
        }

        final List<String> paramList = getParameterList(string);

        return findMethod(paramList, methodsByName);
    }

    @Nullable
    public static PsiMethod fromConstructorString(@NotNull String string, @NotNull PsiClass psiClass) {
        final PsiMethod[] constructors = psiClass.getConstructors();
        if (constructors .length == 0) {
            return null;
        }

        final List<String> parameterList = getParameterList(string);
        return findMethod(parameterList, constructors);
    }

    private static List<String> getParameterList(@NotNull String s) {
        String params = s.substring(s.indexOf('(') + 1, s.lastIndexOf(')'));
        List<String> paramList = Lists.newArrayList();

        int arrayNesting = 0;
        for (int i = 0; i < params.length(); i++) {
            if (params.charAt(i) == '[') {
                arrayNesting++;
                continue;
            }
            if (params.charAt(i) == 'L') {
                String text = McpUtil.normalizeClassString(params.substring(i, params.indexOf(';', i)));
                for (int j = 0; j < arrayNesting; j++) {
                    text = text + "[]";
                }
                paramList.add(text);
                i = params.indexOf(';', i);
                arrayNesting = 0;
            } else {
                paramList.add(String.valueOf(params.charAt(i)));
            }
        }
        return paramList;
    }

    private static PsiMethod findMethod(@NotNull List<String> params, @NotNull PsiMethod[] methods) {
        for (PsiMethod method : methods) {
            final PsiParameter[] parameters = method.getParameterList().getParameters();
            if (params.size() != parameters.length) {
                continue;
            }

            for (int i = 0; i < parameters.length; i++) {
                final PsiParameter parameter = parameters[i];
                if (params.get(i).length() == 1) {
                    if (parameter.getType() != McpUtil.getPrimitiveType(params.get(i))) {
                        return null;
                    }
                } else {
                    if (!parameter.getType().getCanonicalText(false).equals(McpUtil.normalizeClassString(McpUtil.replaceSlashWithDot(params.get(i))))) {
                        return null;
                    }
                }
            }
            // We've passed the check on ever param, this is the method we're looking for
            return method;
        }
        return null;
    }
}
