package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForgeProjectConfiguration extends ProjectConfiguration {

    public List<String> dependencies = new ArrayList<>();
    public String updateUrl;

    public boolean hasDependencies() {
        return listContainsAtLeastOne(dependencies);
    }

    public void setDependencies(String string) {
        this.dependencies.clear();
        Collections.addAll(this.dependencies, commaSplit(string));
    }

    @Override
    public void create(@NotNull Module module, @NotNull PlatformType type, @NotNull BuildSystem buildSystem) {
        ApplicationManager.getApplication().runWriteAction(() -> {

        });
    }
}
