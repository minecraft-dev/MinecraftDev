package com.demonwav.mcdev.settings;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.util.MinecraftTemplate;

import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.io.IOException;
import java.util.List;

public class BungeeCordSettings extends MinecraftSettings {

    public List<String> depend = null;
    public List<String> softDepend = null;

    public boolean hasDepend() {
        return testList(depend);
    }

    public boolean hasSoftDepend() {
        return testList(softDepend);
    }

    @Override
    public void create(Project project, BuildSystem buildSystem) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // Create plugin main class
                VirtualFile file = buildSystem.getSourceDirectory();
                String[] files = buildSystem.getGroupId().split("\\.");
                for (String s : files) {
                    file = file.createChildDirectory(this, s);
                }

                VirtualFile mainClassFile = file.findOrCreateChildData(this, mainClass + ".java");

                MinecraftTemplate.applyMainBungeeCordClassTemplate(project, mainClassFile, buildSystem.getGroupId(), mainClass);
                VirtualFile pluginYml = buildSystem.getResourceDirectory().findOrCreateChildData(this, "plugin.yml");
                MinecraftTemplate.applyBungeeCordPluginYmlTemplate(project, pluginYml, this, buildSystem.getGroupId());

                // Set the editor focus on the main class
                PsiFile mainClassPsi = PsiManager.getInstance(project).findFile(mainClassFile);
                if (mainClassPsi != null) {
                    EditorHelper.openInEditor(mainClassPsi);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
