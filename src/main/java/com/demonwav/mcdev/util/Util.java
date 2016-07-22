package com.demonwav.mcdev.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import org.jetbrains.annotations.NotNull;

public class Util {

    public static void runWriteTask(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(() ->
            ApplicationManager.getApplication().runWriteAction(runnable), ModalityState.any());
    }

    public static void invokeLater(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }
}
