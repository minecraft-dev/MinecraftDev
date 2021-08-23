/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.mixintestdata.shadow;

public class MixinBase {
    private MixinBase() {}

    // Static
    private static final String privateStaticFinalString = "";
    private static String privateStaticString = "";

    protected static final String protectedStaticFinalString = "";
    protected static String protectedStaticString = "";

    static final String packagePrivateStaticFinalString = "";
    static String packagePrivateStaticString = "";

    public static final String publicStaticFinalString = "";
    public static String publicStaticString = "";

    // Non-static
    private final String privateFinalString = "";
    private String privateString = "";

    protected final String protectedFinalString = "";
    protected String protectedString = "";

    final String packagePrivateFinalString = "";
    String packagePrivateString = "";

    public final String publicFinalString = "";
    public String publicString = "";

    // Bad shadows
    protected String wrongAccessor = "";
    protected final String noFinal = "";

    public final String twoIssues = "";

    private static String privateStaticMethod() {
        return null;
    }

    private String privateMethod() {
        return null;
    }
}
