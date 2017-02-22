/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.demonwav.mcdev.platform.AbstractModuleType

abstract class BasePerformanceTest(vararg moduleTypes: AbstractModuleType<*>) : BaseMinecraftTest(*moduleTypes) {

    protected var enabled = true

    override fun setUp() {
        super.setUp()

        if (!System.getProperty("slowCI").isNullOrEmpty()) {
            enabled = false
        }
    }

    fun doTest(test: () -> Unit) {
        if (enabled) {
            test()
        } else {
            println("Skipping ${getTestName(true)} test")
        }
    }
}