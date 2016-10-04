package com.demonwav.mcdev.platform.mixin.actions;

import com.intellij.ide.util.PsiClassListCellRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.ui.components.JBList;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

public class FindMixinsComponent {
    // I need this set before the constructor is called, due to IntelliJ's UI builder
    // So set this statically first
    public static List<PsiClass> classes = null;

    private JPanel panel;
    private JBList classList;

    public JPanel getPanel() {
        return panel;
    }

    private void createUIComponents() {
        classList = new JBList(classes);
        classList.setCellRenderer(PsiClassListCellRenderer.INSTANCE);
        classList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final PsiClass psiClass = (PsiClass) classList.getSelectedValue();
                if (psiClass.canNavigate()) {
                    psiClass.navigate(true);
                }
            }
        });
    }
}
