package com.demonwav.mcdev.platform.liteloader;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.util.CommonColors;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

public class LiteLoaderModuleType extends AbstractModuleType<LiteLoaderModule> {

    private static final String ID = "LITELOADER_MODULE_TYPE";
    private static final LiteLoaderModuleType instance = new LiteLoaderModuleType();

    private LiteLoaderModuleType() {
        super("", "");
        CommonColors.applyStandardColors(this.colorMap, "net.minecraft.util.text.TextFormatting");
    }

    public static LiteLoaderModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.LITELOADER;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.LITELOADER_ICON;
    }

    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public LiteLoaderModule generateModule(Module module) {
        return new LiteLoaderModule(module);
    }

    @Override
    public boolean eventIgnoresCancelled(PsiMethod method) {
        return false; // Not the same as other modules. Events aren't really a thing.
    }
}
