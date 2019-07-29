/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.debug

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.VirtualMachineProxyImpl
import com.intellij.xdebugger.XDebugSessionListener
import com.sun.jdi.BooleanValue
import com.sun.jdi.ClassType

class UngrabMouseDebugSessionListener(private val process: DebugProcessImpl) : XDebugSessionListener {

    private fun grabMouse(grab: Boolean) {
        val suspendContextImpl = process.debuggerContext.suspendContext ?: return
        if (suspendContextImpl.thread?.isAtBreakpoint != true) {
            return
        }

        val frameProxy = suspendContextImpl.frameProxy ?: return
        val debugProcess = suspendContextImpl.debugProcess
        val virtualMachine = debugProcess.virtualMachineProxy as? VirtualMachineProxyImpl ?: return
        val evaluationContext = EvaluationContextImpl(suspendContextImpl, frameProxy)

        val mouseClass = virtualMachine.classesByName("org.lwjgl.input.Mouse")?.singleOrNull() as? ClassType ?: return
        val isGrabbed = mouseClass.methodsByName("isGrabbed", "()Z")?.singleOrNull() ?: return
        val setGrabbed = mouseClass.methodsByName("setGrabbed", "(Z)V")?.singleOrNull() ?: return
        val grabValue = virtualMachine.mirrorOf(grab)

        val currentState = debugProcess.invokeMethod(evaluationContext, mouseClass, isGrabbed, emptyList())
        if ((currentState as? BooleanValue)?.value() != grab) {
            debugProcess.invokeMethod(evaluationContext, mouseClass, setGrabbed, listOf(grabValue))
        }
    }

    override fun sessionPaused() {
        grabMouse(false)
    }
}
