/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;

import com.intellij.codeInspection.ex.EntryPointsManager;
import com.intellij.codeInspection.ex.EntryPointsManagerBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizableStringList;
import com.intellij.psi.PsiClass;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

public abstract class AbstractModuleType<T extends AbstractModule> {

    @NotNull
    private final String groupId;
    @NotNull
    private final String artifactId;
    @NotNull
    protected final LinkedHashMap<String, Color> colorMap = new LinkedHashMap<>();

    public AbstractModuleType(@NotNull final String groupId, @NotNull final String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    @NotNull
    @Contract(pure = true)
    public String getGroupId() {
        return groupId;
    }

    @NotNull
    @Contract(pure = true)
    public String getArtifactId() {
        return artifactId;
    }

    @Contract(pure = true)
    public abstract PlatformType getPlatformType();

    @Contract(pure = true)
    public abstract Icon getIcon();

    @Contract(pure = true)
    public boolean hasIcon() {
        return true;
    }

    @Contract(pure = true)
    public abstract String getId();

    @NotNull
    @Contract(pure = true)
    public abstract List<String> getIgnoredAnnotations();

    @NotNull
    @Contract(pure = true)
    public abstract List<String> getListenerAnnotations();

    @NotNull
    @Contract(pure = true)
    public Map<String, Color> getClassToColorMappings() {
        return this.colorMap;
    }

    @NotNull
    public abstract T generateModule(@NotNull Module module);

    public void performCreationSettingSetup(@NotNull Project project) {
        final JDOMExternalizableStringList annotations = ((EntryPointsManagerBase)EntryPointsManager.getInstance(project)).ADDITIONAL_ANNOTATIONS;
        getIgnoredAnnotations().stream().filter(annotation -> !annotations.contains(annotation)).forEach(annotations::add);
    }

    @NotNull
    @Contract(pure = true)
    public EventGenerationPanel getEventGenerationPanel(@NotNull PsiClass chosenClass) {
        return new EventGenerationPanel(chosenClass);
    }

    @Contract(pure = true)
    public boolean isEventGenAvailable() {
        return false;
    }

    @NotNull
    @Contract(pure = true)
    public String getDefaultListenerName(@NotNull PsiClass psiClass) {
        //noinspection ConstantConditions
        return "on" + psiClass.getName().replace("Event", "");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("groupId", groupId)
            .append("artifactId", artifactId)
            .toString();
    }
}
