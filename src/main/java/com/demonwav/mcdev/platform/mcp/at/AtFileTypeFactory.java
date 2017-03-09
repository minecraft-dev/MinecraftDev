/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at;

import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AtFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        consumer.consume(AtFileType.getInstance(), new FileNameMatcher() {
            @Override
            public boolean accept(@NonNls @NotNull String fileName) {
                return fileName.endsWith("_at.cfg");
            }

            @NotNull
            @Override
            public String getPresentableString() {
                return "Access Transformer";
            }
        });
    }
}
