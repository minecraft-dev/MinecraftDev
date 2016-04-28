/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.util;

import java.util.Iterator;
import java.util.List;

public class ProjectSettings {
    public enum Load {STARTUP, POSTWORLD}

    public String pluginName;
    public String pluginVersion;
    public String mainClass;
    public String description;
    public String author;
    public List<String> authorList;
    public String website;
    public String prefix;
    public boolean database;
    public Load load;
    public List<String> loadBefore;
    public List<String> depend;
    public List<String> softDepend;

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    public boolean hasAuthor() {
        return author != null && !author.trim().isEmpty();
    }

    public boolean hasAuthorList() {
        return testList(authorList);
    }

    public boolean hasWebsite() {
        return website != null && !website.trim().isEmpty();
    }

    public boolean hasDatabase() {
        return database;
    }

    public boolean hasLoad() {
        return load == Load.STARTUP;
    }

    public boolean hasPrefix() {
        return prefix != null && !prefix.trim().isEmpty();
    }

    public boolean hasDepend() {
        return testList(depend);
    }

    public boolean hasSoftDepend() {
        return testList(softDepend);
    }

    public boolean hasLoadBefore() {
        return testList(loadBefore);
    }

    private boolean testList(List<String> list) {
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
}
