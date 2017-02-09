/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JPanel;

public class BungeeCordEventGenerationPanel extends EventGenerationPanel {

    private JComboBox<String> eventPriorityComboBox;
    private JPanel panel;

    public BungeeCordEventGenerationPanel(@NotNull PsiClass chosenClass) {
        super(chosenClass);
    }

    @Nullable
    @Override
    public JPanel getPanel() {
        // Not static because the form builder is not reliable
        eventPriorityComboBox.addItem("HIGHEST");
        eventPriorityComboBox.addItem("HIGH");
        eventPriorityComboBox.addItem("NORMAL");
        eventPriorityComboBox.addItem("LOW");
        eventPriorityComboBox.addItem("LOWEST");

        eventPriorityComboBox.setSelectedIndex(2);

        return panel;
    }

    @Nullable
    @Override
    public GenerationData gatherData() {
        return new BungeeCordGenerationData(eventPriorityComboBox.getSelectedItem().toString());
    }
}
