package com.demonwav.mcdev.platform.bungeecord.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JPanel;

public class BungeeCordEventGenerationPanel extends EventGenerationPanel {

    private JComboBox eventPriorityComboBox;
    private JPanel panel;

    public BungeeCordEventGenerationPanel(@NotNull PsiClass chosenClass) {
        super(chosenClass);
    }

    @Nullable
    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Nullable
    @Override
    public GenerationData gatherData() {
        return new BungeeCordGenerationData(eventPriorityComboBox.getSelectedItem().toString());
    }
}
