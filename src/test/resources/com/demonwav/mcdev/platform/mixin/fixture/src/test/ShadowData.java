/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package test;

@org.spongepowered.asm.mixin.Mixin(targets = "test.MixinBase")
public class ShadowData {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private String privateFinalString;
    @org.spongepowered.asm.mixin.Shadow private String privateString;

    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final protected String protectedFinalString;
    @org.spongepowered.asm.mixin.Shadow protected String protectedString;

    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final String packagePrivateFinalString;
    @org.spongepowered.asm.mixin.Shadow String packagePrivateString;

    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final public String publicFinalString;
    @org.spongepowered.asm.mixin.Shadow public String publicString;

    @org.spongepowered.asm.mixin.Shadow public String wrongAccessor;
    @org.spongepowered.asm.mixin.Shadow public String noFinal;

    @org.spongepowered.asm.mixin.Shadow public String nonExistent;
}
