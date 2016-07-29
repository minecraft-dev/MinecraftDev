package com.demonwav.mcdev.platform.bukkit.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class BukkitEventGenerationPanel extends EventGenerationPanel {

    private JRadioButton ignoreCanceledRadioButton;
    private JPanel panel;
    private JComboBox<String> eventPriorityComboBox;

    public BukkitEventGenerationPanel(@NotNull PsiClass chosenClass) {
        super(chosenClass);
    }

    @Nullable
    @Override
    public JPanel getPanel() {
        ignoreCanceledRadioButton.setSelected(true);

        return panel;
    }

    @Nullable
    @Override
    public GenerationData gatherData() {
        return BukkitGenerationData.builder()
            .ignoreCanceled(ignoreCanceledRadioButton.isSelected())
            .eventPriority(eventPriorityComboBox.getSelectedItem().toString())
            .build();
    }
}
