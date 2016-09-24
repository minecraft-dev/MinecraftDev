package com.demonwav.mcdev.inspection;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.siyeh.ig.InspectionGadgetsFix;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class IsCancelled {

    private final InspectionGadgetsFix buildFix;
    private final String errorString;

    public IsCancelled(InspectionGadgetsFix buildFix, String errorString) {
        this.buildFix = buildFix;
        this.errorString = errorString;
    }

    public InspectionGadgetsFix getBuildFix() {
        return buildFix;
    }

    public String getErrorString() {
        return errorString;
    }

    public static IsCancelledBuilder builder() {
        return new IsCancelledBuilder();
    }

    public static class IsCancelledBuilder {
        private InspectionGadgetsFix fix;
        private String errorString;

        public IsCancelledBuilder setFix(final DoFix fix) {
            this.fix = new InspectionGadgetsFix() {
                @Override
                protected void doFix(Project project, ProblemDescriptor descriptor) {
                    fix.doFix(descriptor);
                }

                @Nls
                @NotNull
                @Override
                public String getName() {
                    return "Simplify";
                }

                @Nls
                @NotNull
                @Override
                public String getFamilyName() {
                    return "Useless Is Cancelled Check";
                }
            };
            return this;
        }

        public IsCancelledBuilder setErrorString(String errorString) {
            this.errorString = errorString;
            return this;
        }

        public IsCancelled build() {
            return new IsCancelled(fix, errorString);
        }
    }

    public interface DoFix {
        void doFix(ProblemDescriptor descriptor);
    }
}
