/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.pluginyaml;

import com.demonwav.bukkitplugin.BukkitProject;
import com.demonwav.bukkitplugin.util.BukkitUtil;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.impl.YAMLFileImpl;

import java.util.List;

public class PluginConfigManager {

    @NotNull
    private BukkitProject project;
    @NotNull
    private PluginConfig config;

    public PluginConfigManager(@NotNull BukkitProject project) {
        // TODO: This doesn't setup when a project is opened
        this.project = project;
        this.config = new PluginConfig(project);

        reimportConfig();

        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
            @Override
            public void contentsChanged(@NotNull VirtualFileEvent event) {
                if (event.getFile().getName().equals("plugin.yml")) {
                    reimportConfig();
                }
            }
        });
    }

    @NotNull
    public PluginConfig getConfig() {
        return config;
    }

    private void reimportConfig() {
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiFile pluginYml = BukkitUtil.getPluginYml(project);
            if (BukkitUtil.isUltimate()) {
                YAMLFile file = ((YAMLFileImpl) pluginYml);

                if (file == null)
                    return; // TODO: Show warning to user

                // TODO: Show warning to user if there is more than one document
                YAMLDocument document = file.getDocuments().get(0);
                document.getYAMLElements().forEach(e -> {
                    System.out.println(e.toString());
                    if (e.getYAMLElements().size() != 0)
                        printYamlEles(e.getYAMLElements(), 1);
                });
                System.out.println();
            }
        });
    }

    private void printYamlEles(List<YAMLPsiElement> elementList, int indent) {
        elementList.forEach(e -> {
            for (int i = 0; i < indent; i++) {
                System.out.print("    ");
            }
            System.out.println(e.toString());
            if (e.getChildren().length != 0)
                printYamlEles(e.getYAMLElements(), indent + 1);
        });
    }
}
