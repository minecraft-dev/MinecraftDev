/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.framework.BaseMinecraftTest

class AtParserPerformanceTest : BaseMinecraftTest() {

    override fun getTestDataPath() = "src/test/resources/com/demonwav/mcdev/platform/mcp/at/performance/fixtures"

    fun testHighlightingPerformance() {
        myFixture.configureByFile("big_at.cfg")
        val elapsed = myFixture.checkHighlighting()
        // elapsed is milliseconds, file is 10,000 lines long
        // shouldn't take more than 500 ms to complete
        assertTrue(elapsed < 1000)
    }
}
