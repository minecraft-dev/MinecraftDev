package com.demonwav.mcdev.creator;


import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import org.apache.commons.lang.WordUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SpongeProjectSettingsWizard extends ModuleWizardStep {

    private final MavenProjectCreator creator;
    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JPanel panel;
    private JLabel title;

    public SpongeProjectSettingsWizard(MavenProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        pluginNameField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());
        mainClassField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));

        return panel;
    }

    @Override
    public void updateDataModel() {

    }

    @Override
    public void onStepLeaving() {
        super.onStepLeaving();
    }
}
