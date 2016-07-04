package com.demonwav.mcdev.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;

public class Util {

    public static void runWriteTask(Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(() -> ApplicationManager.getApplication().runWriteAction(runnable), ModalityState.any());
    }

    public static void invokeLater(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }
}
