/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;
import com.intellij.psi.PsiClass;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CanaryHookGenerationPanel extends EventGenerationPanel {

    private JRadioButton ignoreCanceledRadioButton;
    private JPanel panel;
    private JComboBox<String> hookPriorityComboBox;

    public CanaryHookGenerationPanel(@NotNull PsiClass chosenClass) {
        super(chosenClass);
    }

    @Nullable
    @Override
    public JPanel getPanel() {
        ignoreCanceledRadioButton.setSelected(true);

        // Not static because the form builder is not reliable
        hookPriorityComboBox.addItem("PASSIVE");
        hookPriorityComboBox.addItem("LOW");
        hookPriorityComboBox.addItem("NORMAL");
        hookPriorityComboBox.addItem("HIGH");
        hookPriorityComboBox.addItem("CRITICAL");

        hookPriorityComboBox.setSelectedIndex(3);

        return panel;
    }

    @Nullable
    @Override
    public GenerationData gatherData() {
        return new CanaryGenerationData(ignoreCanceledRadioButton.isSelected(), hookPriorityComboBox.getSelectedItem().toString());
    }
}
