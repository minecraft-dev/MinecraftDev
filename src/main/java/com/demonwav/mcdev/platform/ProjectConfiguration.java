package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.buildsystem.BuildSystem;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class ProjectConfiguration {

    public String pluginName = null;
    public String pluginVersion = null;
    public String mainClass = null;
    public String description = null;
    public final List<String> authors = new ArrayList<>();
    public String website = null;
    public PlatformType type = null;

    public abstract void create(@NotNull Module module, @NotNull BuildSystem buildSystem, @NotNull ProgressIndicator indicator);

    public boolean hasAuthors() {
        return listContainsAtLeastOne(this.authors);
    }

    public void setAuthors(String string) {
        this.authors.clear();
        Collections.addAll(this.authors, commaSplit(string));
    }

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    @NotNull
    protected static String[] commaSplit(@NotNull String string) {
        return string.trim().replaceAll("\\[|\\]", "").split("\\s*,\\s*");
    }

    @Contract("null -> false")
    protected static boolean listContainsAtLeastOne(List<String> list) {
        if (list == null || list.size() == 0) {
            return false;
        }

        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if (s.trim().isEmpty()) {
                it.remove();
            }
        }

        return list.size() != 0;
    }
}
