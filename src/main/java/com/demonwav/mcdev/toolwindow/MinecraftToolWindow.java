/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.toolwindow;

import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.containers.OrderedSet;
import org.jetbrains.annotations.NotNull;

import java.awt.Insets;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class MinecraftToolWindow {
    private JPanel panel;

    private Project project;
    private OrderedSet<MinecraftModule> mcModules = new OrderedSet<>();

    public void setProjectAndInit(@NotNull Project project) {
        this.project = project;

        final Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            final MinecraftModule instance = MinecraftModule.getInstance(module);
            if (instance != null) {
                mcModules.add(instance);
            }
        }

        Collections.sort(mcModules, (m1, m2) -> m1.getIdeaModule().getName().compareTo(m2.getIdeaModule().getName()));

        panel.setLayout(new GridLayoutManager(mcModules.size() + 1, 1));
        final GridLayoutManager layout = (GridLayoutManager) panel.getLayout();
        layout.setMargin(new Insets(10, 10, 10, 10));
        layout.setHGap(10);
        layout.setVGap(10);

        int i = 0;
        for (MinecraftModule module : mcModules) {
            final String name = module.getIdeaModule().getName();

            final JLabel jLabel = new JLabel(name);
            jLabel.setIcon(module.getIcon());

            final GridConstraints constraints = new GridConstraints();
            constraints.setRow(i++);
            constraints.setAnchor(GridConstraints.ANCHOR_WEST);

            panel.add(jLabel, constraints);
        }

        final GridConstraints constraints = new GridConstraints();
        constraints.setRow(i);
        panel.add(new Spacer(), new GridConstraints(i, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    public JPanel getPanel() {
        return panel;
    }
}
