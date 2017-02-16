/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight.generation.ui;

import com.intellij.ide.highlighter.JavaHighlightingColors;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.wm.ex.IdeFocusTraversalPolicy;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.UIUtil;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EventListenerWizard {
    private JPanel panel;
    private JTextField classNameTextField;
    // TODO: This field isn't always focusable when this wizard is created. I have no idea why or how to fix it, but it needs to be fixed before release
    private JTextField listenerNameTextField;
    private JLabel publicVoidLabel;
    private JPanel contentPanel;
    private JSeparator separator;

    private static final GridConstraints innerContentPanelConstraints = new GridConstraints();
    static {
        innerContentPanelConstraints.setRow(0);
        innerContentPanelConstraints.setColumn(0);
        innerContentPanelConstraints.setRowSpan(1);
        innerContentPanelConstraints.setColSpan(1);
        innerContentPanelConstraints.setAnchor(GridConstraints.ANCHOR_CENTER);
        innerContentPanelConstraints.setFill(GridConstraints.FILL_BOTH);
        innerContentPanelConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        innerContentPanelConstraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
    }

    public EventListenerWizard(@Nullable JPanel panel, @NotNull String className, @NotNull String defaultListenerName) {
        classNameTextField.setFont(EditorUtil.getEditorFont());
        listenerNameTextField.setFont(EditorUtil.getEditorFont());
        publicVoidLabel.setFont(EditorUtil.getEditorFont());
        if (UIUtil.isUnderDarcula()) {
            publicVoidLabel.setForeground(JavaHighlightingColors.KEYWORD.getDefaultAttributes().getForegroundColor());
        } else {
            publicVoidLabel.setForeground(JavaHighlightingColors.KEYWORD.getFallbackAttributeKey().getDefaultAttributes().getForegroundColor());
        }

        if (panel != null) {
            separator.setVisible(true);
            contentPanel.add(panel, innerContentPanelConstraints);
        }

        classNameTextField.setText(className);
        listenerNameTextField.setText(defaultListenerName);

        IdeFocusTraversalPolicy.getPreferredFocusedComponent(listenerNameTextField).requestFocus();
        listenerNameTextField.requestFocus();
    }

    public JPanel getPanel() {
        return panel;
    }

    public String getChosenClassName() {
        return listenerNameTextField.getText();
    }
}
