package com.demonwav.mcdev.inspection;

import com.siyeh.ig.InspectionGadgetsFix;

public class IsCancelled {

    private InspectionGadgetsFix buildFix;
    private String errorString;

    public InspectionGadgetsFix getBuildFix() {
        return buildFix;
    }

    public void setBuildFix(InspectionGadgetsFix buildFix) {
        this.buildFix = buildFix;
    }

    public String getErrorString() {
        return errorString;
    }

    public void setErrorString(String errorString) {
        this.errorString = errorString;
    }
}
