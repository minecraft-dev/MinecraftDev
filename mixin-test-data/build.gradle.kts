/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

plugins {
    java
}

tasks.withType<JavaCompile>().configureEach {
    options.debugOptions.debugLevel = "vars"
}
