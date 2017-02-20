/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.ColorIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColorPicker {

    @NotNull
    private final JPanel panel = new JPanel(new GridBagLayout());

    private String chosenColor;

    @NotNull
    private final Map<String, Color> colorMap;
    @NotNull
    private final ColorPickerDialog dialog;

    public ColorPicker(@NotNull Map<String, Color> colorMap, @NotNull Component parent) {
         dialog = new ColorPickerDialog(parent, panel);

        this.colorMap = colorMap;
    }

    @Nullable
    public String showDialog() {
        init();

        dialog.show();

        return chosenColor;
    }

    private void init() {
        Iterator<Map.Entry<String, Color>> iterator = colorMap.entrySet().iterator();
        addToPanel(0, 8, panel, iterator);
        addToPanel(1, 8, panel, iterator);
    }

    private void addToPanel(int row, int cols, @NotNull JPanel panel, @NotNull Iterator<Map.Entry<String, Color>> iterator) {
        for (int i = 0; i < cols; i++) {
            if (!iterator.hasNext()) {
                break;
            }

            final Map.Entry<String, Color> entry = iterator.next();
            final ColorIcon icon = new ColorIcon(28, entry.getValue(), true);

            final JLabel label = new JLabel(icon);
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    chosenColor = entry.getKey();
                    dialog.close(0);
                }
            });

            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridy = row;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets(10, 10, 10, 10);

            panel.add(
                label,
                constraints
            );
        }
    }

    private static class ColorPickerDialog extends DialogWrapper {

        private final JComponent component;

        ColorPickerDialog(@NotNull Component parent, @NotNull JComponent component) {
            super(parent, false);

            this.component = component;

            setTitle("Choose Color");
            setResizable(true);

            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return component;
        }
    }
}
