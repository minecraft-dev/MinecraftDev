package com.demonwav.mcdev.insight.generation.ui;

import com.demonwav.mcdev.insight.generation.GenerationData;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;

/**
 * Base class for the more-info event generation panel, to be overridden by the platforms. By default this class does nothing, shows no
 * panel, does no validation, and gathers no data.
 */
public class EventGenerationPanel {

    protected PsiClass chosenClass;

    public EventGenerationPanel(@NotNull PsiClass chosenClass) {
        this.chosenClass = chosenClass;
    }

    public PsiClass getChosenClass() {
        return chosenClass;
    }

    /**
     * Return the base panel for this dialog. This should be the root display element, and it should contain whatever JComponents are needed
     * by the platform. If this method returns null then no panel will be shown.
     *
     * @return The main panel to display, or null if nothing should be displayed.
     */
    @Nullable
    public JPanel getPanel() {
        return null;
    }

    /**
     * This is called when the dialog is closing from the OK action. The platform should fill in their {@link GenerationData} object as
     * needed for whatever information their panel provides. The state of the panel can be assumed to be valid, since this will only be
     * called if {@link #doValidate()} has passed successfully.
     *
     * @return The {@link GenerationData} object which will be passed to the
     * {@link com.demonwav.mcdev.platform.AbstractModule#doPreEventGenerate(PsiClass, GenerationData) AbstractModule#doPreEventGenerate()} and
     * {@link com.demonwav.mcdev.platform.AbstractModule#generateEventListenerMethod(PsiClass, PsiClass, String, GenerationData) AbstractModule#generateEventListenerMethod}
     * methods.
     */
    @Nullable
    public GenerationData gatherData() {
        return null;
    }

    /**
     * Validate the user input in the panel. Returns null when there are no errors, otherwise returns a {@link ValidationInfo} object filled
     * in with whatever relevant error data is necessary.
     *
     * @return The relevant {@link ValidationInfo} object, or null if there are no errors.
     */
    @Nullable
    public ValidationInfo doValidate() {
        return null;
    }
}
