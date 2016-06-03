package com.demonwav.mcdev.asset;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

public final class MessageAssets {

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    private static Reference<ResourceBundle> ourBundle;
    @NonNls
    private static final String BUNDLE = "messages.MinecraftDevelopment";

    private MessageAssets() {
    }

    public static String getGenerateEventListenerTitle() {
        return message("generate.event_listener");
    }

    public static String getSearchForTextOccurrencesText() {
        return message("search.for.text.occurrences");
    }

    public static String getVisibilityPackageLocal() {
        return message("visibility.package.local");
    }

    public static String getVisibilityPrivate() {
        return message("visibility.private");
    }

    public static String getVisibilityProtected() {
        return message("visibility.protected");
    }

    public static String getVisibilityPublic() {
        return message("visibility.public");
    }

    public static String getVisibilityAsIs() {
        return message("visibility.as.is");
    }

    public static String getEscalateVisibility() {
        return message("visibility.escalate");
    }

    public static String getCannotRefactorMessage(@Nullable final String message) {
        return message("cannot.perform.refactoring") + (message == null ? "" : "\n" + message);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key) {
        return CommonBundle.message(getBundle(), key);
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(ourBundle);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            ourBundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }
}
