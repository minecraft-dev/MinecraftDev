/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class BukkitEventGenerationPanel extends EventGenerationPanel {

    private JCheckBox ignoreCanceledCheckBox;
    private JPanel panel;
    private JComboBox<String> eventPriorityComboBox;

    public BukkitEventGenerationPanel(@NotNull PsiClass chosenClass) {
        super(chosenClass);
    }

    @Nullable
    @Override
    public JPanel getPanel() {
        ignoreCanceledCheckBox.setSelected(true);

        // Not static because the form builder is not reliable
        eventPriorityComboBox.addItem("MONITOR");
        eventPriorityComboBox.addItem("HIGHEST");
        eventPriorityComboBox.addItem("HIGH");
        eventPriorityComboBox.addItem("NORMAL");
        eventPriorityComboBox.addItem("LOW");
        eventPriorityComboBox.addItem("LOWEST");

        eventPriorityComboBox.setSelectedIndex(3);

        return panel;
    }

    @Nullable
    @Override
    public GenerationData gatherData() {
        return new BukkitGenerationData(ignoreCanceledCheckBox.isSelected(), eventPriorityComboBox.getSelectedItem().toString());
    }
}
