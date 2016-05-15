package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.buildsystem.BuildSystem;

import com.intellij.openapi.module.Module;
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

    public abstract void create(@NotNull Module module, @NotNull PlatformType type, @NotNull BuildSystem buildSystem);

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

    protected static String[] commaSplit(String string) {
        return string.trim().replaceAll("\\[|\\]", "").split("\\s*,\\s*");
    }

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
