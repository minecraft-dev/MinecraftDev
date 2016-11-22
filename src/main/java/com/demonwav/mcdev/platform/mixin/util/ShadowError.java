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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ShadowError {

    private Key key;
    private Object[] errorContextInfos;
    private Level level;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public Object[] getErrorContextInfos() {
        return errorContextInfos;
    }

    public void setErrorContextInfos(Object[] errorContextInfos) {
        this.errorContextInfos = errorContextInfos;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private  Key key;
        private List<Object> errorContextInfos = Lists.newArrayList();
        private Level level = Level.ERROR;

        public ShadowError build() {
            ShadowError error = new ShadowError();
            error.setKey(key);
            error.setErrorContextInfos(errorContextInfos.toArray());
            error.setLevel(level);
            return error;
        }

        public Builder setError(Key key) {
            this.key = key;
            return this;
        }

        public Builder addContext(Object context) {
            errorContextInfos.add(context);
            return this;
        }

        public Builder setLevel(Level level) {
            this.level = level;
            return this;
        }
    }

    public enum Level {
        SOFT_WARNING(ProblemHighlightType.WEAK_WARNING),
        WARNING(ProblemHighlightType.GENERIC_ERROR_OR_WARNING),
        ERROR(ProblemHighlightType.GENERIC_ERROR);

        private final ProblemHighlightType type;

        Level(ProblemHighlightType type) {
            this.type = type;
        }

        public ProblemHighlightType getHighlightType() {
            return type;
        }
    }

    public enum Key {
        MULTI_TARGET_CLASS_REMAPPED_TRUE,
        MULTI_TARGET_SHADOW_REMAPPED_TRUE,
        NO_SHADOW_METHOD_FOUND_WITH_REMAP,
        INVALID_ACCESSOR_ON_SHADOW_METHOD,
        CANNOT_FIND_MIXIN_TARGET,
        NOT_MIXIN_CLASS,
        NO_MIXIN_CLASS_TARGETS,
        NO_SHADOW_FIELD_FOUND_WITH_REMAP,
        INVALID_ACCESSOR_ON_SHADOW_FIELD,
        INVALID_FIELD_TYPE,
        NO_FINAL_ANNOTATION_WITH_FINAL_TARGET
    }

    public String formatError() {
        if (errorContextInfos.length == 0) {
            return "Error";
        }

        return FORMATTERS.stream()
            .filter(formatter -> formatter.matches(getKey()))
            .findFirst()
            .map(found -> found.formatMessage(getErrorContextInfos()))
            .orElse("Error");
    }

    @Nullable
    public InspectionGadgetsFix fixError() {
        if (errorContextInfos.length == 0) {
            return null;
        }

        return FIXERS.stream()
            .filter(fixer -> fixer.matches(getKey()))
            .findFirst()
            .map(found -> found.getFix(getErrorContextInfos()))
            .orElse(null);
    }

    private static final List<Formatter> FORMATTERS = ImmutableList.<Formatter>builder()
        .add(new Formatter(Key.MULTI_TARGET_CLASS_REMAPPED_TRUE) {
            @Override
            String formatMessage(Object... args) {
                final PsiClass containingClass = (PsiClass) args[0];
                final String containingName = containingClass.getName();

                return "Cannot have a shadow when " + containingName + " is mixing into multiple remapped targets.";
            }
        })
        .add(new Formatter(Key.MULTI_TARGET_SHADOW_REMAPPED_TRUE) {
            @Override
            String formatMessage(Object... args) {
                final PsiClass containingClass = (PsiClass) args[0];
                final String containingName = containingClass.getName();

                return "Cannot have a remapped shadow when " + containingName + " is mixing into multiple targets.";
            }
        })
        .add(new Formatter(Key.NO_SHADOW_METHOD_FOUND_WITH_REMAP) {
            @Override
            String formatMessage(Object... args) {
                final String methodName = (String) args[0];
                final String targetClassName = (String) args[1];

                return "No method found by the name: " + methodName + " in target classes: " + targetClassName;
            }
        })
        .add(new Formatter(Key.INVALID_ACCESSOR_ON_SHADOW_METHOD) {
            @Override
            String formatMessage(Object... args) {
                final String current = (String) args[0];
                final String expected = (String) args[1];
                return "Method has invalid access modifiers, has: " + current + " but target method has: " + expected + ".";
            }
        })
        .add(new Formatter(Key.CANNOT_FIND_MIXIN_TARGET) {
            @Override
            String formatMessage(Object... args) {
                return "Cannot shadow nonexistent target: target class undefined";
            }
        })
        .add(new Formatter(Key.NOT_MIXIN_CLASS) {
            @Override
            String formatMessage(Object... args) {
                return "Cannot shadow anything in a non-@Mixin annotated class.";
            }
        })
        .add(new Formatter(Key.NO_MIXIN_CLASS_TARGETS) {
            @Override
            String formatMessage(Object... args) {
                return "Cannot shadow anything when the Mixin class has no targets.";
            }
        })
        .add(new Formatter(Key.NO_SHADOW_FIELD_FOUND_WITH_REMAP) {
            @Override
            String formatMessage(Object... args) {
                final String methodName = (String) args[0];
                final String targetClassName = (String) args[1];

                return "No field found by the name: " + methodName + " in target classes: " + targetClassName;
            }
        })
        .add(new Formatter(Key.INVALID_ACCESSOR_ON_SHADOW_FIELD) {
            @Override
            String formatMessage(Object... args) {
                final String current = (String) args[0];
                final String expected = (String) args[1];
                return "Field has invalid access modifiers, has: " + current + " but target field has: " + expected + ".";
            }
        })
        .add(new Formatter(Key.INVALID_FIELD_TYPE) {
            @Override
            String formatMessage(Object... args) {
                final String fieldType = (String) args[0];
                final String targetType = (String) args[1];
                return "Field has invalid type, has " + fieldType + " but target field has " + targetType + ".";
            }
        })
        .add(new Formatter(Key.NO_FINAL_ANNOTATION_WITH_FINAL_TARGET) {
            @Override
            String formatMessage(Object... args) {
                return "Field targeting a final target field is not annotated with @Final";
            }
        })
        .build();

    private static final List<Fixer> FIXERS = ImmutableList.<Fixer>builder()
        .add(new Fixer(Key.INVALID_ACCESSOR_ON_SHADOW_METHOD) {
            @Override
            InspectionGadgetsFix getFix(Object... args) {
                return new ShadowFix("Fix invalid accessor") {
                    @Override
                    protected void doFix(Project project, ProblemDescriptor descriptor) {
                        final String current = (String) args[0];
                        String expected = (String) args[1];
                        final PsiMethod method = (PsiMethod) args[2];

                        if (!current.equals(PsiModifier.PACKAGE_LOCAL)) {
                            method.getModifierList().setModifierProperty(current, false);
                        }
                        if (expected.equals("private")) {
                            expected = "protected";
                        }
                        method.getModifierList().setModifierProperty(expected, true);
                    }
                };
            }
        })
        .add(new Fixer(Key.INVALID_ACCESSOR_ON_SHADOW_FIELD) {
            @Override
            InspectionGadgetsFix getFix(Object... args) {
                return new ShadowFix("Fix invalid accessor") {
                    @Override
                    protected void doFix(Project project, ProblemDescriptor descriptor) {
                        final String current = (String) args[0];
                        final String expected = (String) args[1];
                        final PsiField field = (PsiField) args[2];

                        final PsiModifierList modifierList = field.getModifierList();
                        if (modifierList != null) {
                            if (!current.equals(PsiModifier.PACKAGE_LOCAL)) {
                                modifierList.setModifierProperty(current, false);
                            }
                            modifierList.setModifierProperty(expected, true);
                        }
                    }
                };
            }
        })
        .add(new Fixer(Key.INVALID_FIELD_TYPE) {
            @Override
            InspectionGadgetsFix getFix(Object... args) {
                return new ShadowFix("Fix invalid type") {
                    @Override
                    protected void doFix(Project project, ProblemDescriptor descriptor) {
                        final PsiField field = (PsiField) args[2];
                        final PsiType type = (PsiType) args[3];

                        final PsiTypeElement newType = JavaPsiFacade.getElementFactory(project).createTypeElement(type);
                        final PsiTypeElement oldType = field.getTypeElement();
                        if (oldType != null) {
                            oldType.replace(newType);
                        }
                    }
                };
            }
        })
        .add(new Fixer(Key.NO_FINAL_ANNOTATION_WITH_FINAL_TARGET) {
            @Override
            InspectionGadgetsFix getFix(Object... args) {
                return new ShadowFix("Add @Final annotation") {
                    @Override
                    protected void doFix(Project project, ProblemDescriptor descriptor) {
                        final PsiField field = (PsiField) args[0];

                        final PsiModifierList modifierList = field.getModifierList();
                        if (modifierList == null) {
                            return;
                        }

                        modifierList.addAnnotation(MixinConstants.Annotations.FINAL);
                    }
                };
            }
        })
        .build();


    private static abstract class Formatter {
        final Key key;

        Formatter(Key key) {
            this.key = key;
        }

        boolean matches(Key key) {
            return this.key == key;
        }

        abstract String formatMessage(Object... args);
    }

    private static abstract class Fixer {
        private final Key key;

        Fixer(Key key) {
            this.key = key;
        }

        boolean matches(Key key) {
            return this.key == key;
        }

        abstract InspectionGadgetsFix getFix(Object... args);
    }

    private static abstract class ShadowFix extends InspectionGadgetsFix {
        private final String name;
        public ShadowFix(String name) {
            this.name = name;
        }

        @Nls
        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @Nls
        @NotNull
        @Override
        public String getFamilyName() {
            return "Shadow quick fix";
        }
    }
}
