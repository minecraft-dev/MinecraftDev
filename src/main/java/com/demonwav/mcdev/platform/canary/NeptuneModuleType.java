/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.canary.util.CanaryConstants;
import com.demonwav.mcdev.util.CommonColors;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

public class NeptuneModuleType extends CanaryModuleType {

    private static final NeptuneModuleType instance = new NeptuneModuleType();

    private static final String ID = "NEPTUNE_MODULE_TYPE";

    private NeptuneModuleType() {
        super("org.neptunepowered", "NeptuneLib");
        CommonColors.applyStandardColors(this.colorMap, CanaryConstants.MCP_CHAT_FORMATTING);
    }

    @NotNull
    public static NeptuneModuleType getInstance() {
        return instance;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.NEPTUNE_ICON;
    }

}
