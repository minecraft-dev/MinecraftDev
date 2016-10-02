package com.demonwav.mcdev.platform.canary;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.canary.util.CanaryConstants;
import com.demonwav.mcdev.platform.canary.util.CanaryLegacyColors;
import com.demonwav.mcdev.util.CommonColors;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

public class CanaryModuleType extends AbstractModuleType<CanaryModule> {

    private static final String ID = "CANARY_MODULE_TYPE";
    private static final CanaryModuleType instance = new CanaryModuleType();

    private CanaryModuleType() {
        super("net.canarymod", "CanaryLib");
        CommonColors.applyStandardColors(this.colorMap, CanaryConstants.CHAT_FORMAT_CLASS);
        CanaryLegacyColors.applyStandardColors(this.colorMap, CanaryConstants.LEGACY_COLORS_CLASS);
        CanaryLegacyColors.applyStandardColors(this.colorMap, CanaryConstants.LEGACY_TEXT_FORMAT_CLASS);
    }

    protected CanaryModuleType(final String groupId, final String artifactId) {
        super(groupId, artifactId);
        CommonColors.applyStandardColors(this.colorMap, CanaryConstants.CHAT_FORMAT_CLASS);
        CanaryLegacyColors.applyStandardColors(this.colorMap, CanaryConstants.LEGACY_COLORS_CLASS);
        CanaryLegacyColors.applyStandardColors(this.colorMap, CanaryConstants.LEGACY_TEXT_FORMAT_CLASS);
    }

    @NotNull
    public static CanaryModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.CANARY;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ImmutableList.of(
                CanaryConstants.HANDLER_ANNOTATION,
                CanaryConstants.COMMAND_ANNOTATION,
                CanaryConstants.TABCOMPLETE_ANNOTATION,
                CanaryConstants.COLUMN_ANNOTATION
        );
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of(CanaryConstants.HANDLER_ANNOTATION);
    }

    @NotNull
    @Override
    public CanaryModule generateModule(Module module) {
        return new CanaryModule(module, this);
    }

}
