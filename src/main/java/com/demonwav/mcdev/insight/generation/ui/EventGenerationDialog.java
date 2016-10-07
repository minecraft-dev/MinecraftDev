/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.insight.generation.ui;

import com.demonwav.mcdev.insight.generation.GenerationData;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class EventGenerationDialog extends DialogWrapper {

    private final EventGenerationPanel panel;
    private EventListenerWizard wizard;
    private GenerationData data;

    public EventGenerationDialog(@NotNull Editor editor,
                                 @NotNull EventGenerationPanel panel,
                                 @NotNull String className,
                                 @NotNull String defaultListenerName) {
        super(editor.getComponent(), false);

        this.panel = panel;
        //noinspection ConstantConditions
        this.wizard = new EventListenerWizard(panel.getPanel(), className, defaultListenerName);

        setTitle("Event Listener Settings");
        setOKActionEnabled(true);
        setValidationDelay(0);

        init();
    }

    @Override
    protected void doOKAction() {
        data = panel.gatherData();
        super.doOKAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return wizard.getPanel();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return panel.doValidate();
    }

    public GenerationData getData() {
        return data;
    }

    public String getChosenName() {
        return wizard.getChosenClassName();
    }
}
