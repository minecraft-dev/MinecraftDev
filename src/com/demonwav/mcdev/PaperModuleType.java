package com.demonwav.mcdev;

import com.intellij.openapi.module.ModuleTypeManager;

public class PaperModuleType extends SpigotModuleType {

    private static final String ID = "PAPER_MODULE_TYPE";

    public PaperModuleType() {
        super(ID);
    }

    public static PaperModuleType getInstance() {
        return (PaperModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    // TODO: add icons
}
