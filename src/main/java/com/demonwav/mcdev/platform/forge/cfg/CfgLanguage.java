package com.demonwav.mcdev.platform.forge.cfg;

import com.intellij.lang.Language;

public class CfgLanguage extends Language{
    private static final CfgLanguage instance = new CfgLanguage();

    public static CfgLanguage getInstance() {
        return instance;
    }

    private CfgLanguage() {
        super("cfg");
    }
}
