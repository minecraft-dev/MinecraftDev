package com.demonwav.mcdev.platform.bukkit.macro

import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Result
import com.intellij.codeInsight.template.TextResult
import com.intellij.codeInsight.template.macro.MacroBase

class BukkitEventNameMacro : MacroBase("bukkitEventName", "Usage: bukkitEventName(ClassType)") {
    override fun calculateResult(
        params: Array<out Expression?>,
        context: ExpressionContext?,
        quick: Boolean
    ): Result? {
        val text = params[0]
            ?.calculateResult(context)
            ?.toString() ?: return null

        val result = text.split("\\.").last().removeSuffix("Event")
        return TextResult(result)
    }
}
