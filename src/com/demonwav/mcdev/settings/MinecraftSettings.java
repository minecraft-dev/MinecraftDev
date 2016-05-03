package com.demonwav.mcdev.settings;

import com.demonwav.mcdev.buildsystem.BuildSystem;

import com.intellij.openapi.project.Project;

import java.util.Iterator;
import java.util.List;

public abstract class MinecraftSettings {

    public String pluginName = null;
    public String pluginVersion = null;
    public String mainClass = null;
    public String description = null;
    public String author = null;
    public String website = null;

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    public boolean hasAuthor() {
        return author != null && !author.trim().isEmpty();
    }

    protected boolean testList(List<String> list) {
        if (list == null || list.size() == 0)
            return false;

        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if (s.trim().isEmpty())
                it.remove();
        }

        return list.size() != 0;
    }

    public abstract void create(Project project, BuildSystem buildSystem);
}
