/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.actions;

import com.intellij.ide.util.PsiClassListCellRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

public class FindMixinsComponent {
    private JPanel panel;
    private JBList classList;

    public FindMixinsComponent(@NotNull List<PsiClass> classes) {
        //noinspection unchecked
        classList.setModel(JBList.createDefaultListModel(classes.toArray()));
        classList.setCellRenderer(PsiClassListCellRenderer.INSTANCE);

        classList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final PsiClass psiClass = (PsiClass) classList.getSelectedValue();
                if (psiClass != null && psiClass.canNavigate()) {
                    psiClass.navigate(true);
                }
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }
}
