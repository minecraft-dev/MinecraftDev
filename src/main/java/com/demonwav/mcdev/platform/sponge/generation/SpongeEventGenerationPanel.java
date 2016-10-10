/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.generation;

import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class SpongeEventGenerationPanel extends EventGenerationPanel {

    private JPanel panel;
    private JComboBox<String> eventOrderComboBox;
    private JCheckBox ignoreCanceledCheckBox;

    public SpongeEventGenerationPanel(@NotNull PsiClass chosenClass) {
        super(chosenClass);
    }

    @Nullable
    @Override
    public JPanel getPanel() {
        ignoreCanceledCheckBox.setSelected(true);

        // Not static because the form builder is not reliable
        eventOrderComboBox.addItem("PRE");
        eventOrderComboBox.addItem("AFTER_PRE");
        eventOrderComboBox.addItem("FIRST");
        eventOrderComboBox.addItem("EARLY");
        eventOrderComboBox.addItem("DEFAULT");
        eventOrderComboBox.addItem("LATE");
        eventOrderComboBox.addItem("LAST");
        eventOrderComboBox.addItem("BEFORE_POST");
        eventOrderComboBox.addItem("POST");

        eventOrderComboBox.setSelectedIndex(4);

        return panel;
    }

    @Nullable
    @Override
    public ValidationInfo doValidate() {
        return super.doValidate();
    }

    @Nullable
    @Override
    public GenerationData gatherData() {
        return new SpongeGenerationData(ignoreCanceledCheckBox.isSelected(), (String) eventOrderComboBox.getSelectedItem());
    }
}
