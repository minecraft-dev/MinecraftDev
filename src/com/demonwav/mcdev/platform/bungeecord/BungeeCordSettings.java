package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.MinecraftSettings;
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
                String[] files = this.mainClass.split("\\.");
                String name = files[files.length - 1];
                for (int i = 0, len = files.length - 1; i < len; i++) {
                    String s = files[i];
                    file = file.createChildDirectory(this, s);
                }

                VirtualFile mainClassFile = file.findOrCreateChildData(this, name + ".java");

                MinecraftTemplate.applyMainBungeeCordClassTemplate(project, mainClassFile, buildSystem.getGroupId(), name);
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
