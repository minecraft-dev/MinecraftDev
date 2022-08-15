/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.demonwav.mcdev.util.edtCoroutineScope
import java.lang.reflect.Method
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext

class EdtInterceptor : InvocationInterceptor {
    override fun interceptBeforeEachMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        exec(invocation, invocationContext)
    }

    override fun interceptAfterEachMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        exec(invocation, invocationContext)
    }

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        exec(invocation, invocationContext)
    }

    private fun exec(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>
    ) {
        if (invocationContext.executable.getAnnotation(NoEdt::class.java) != null) {
            invocation.proceed()
            return
        }

        val thrown = edtCoroutineScope {
            runCatching {
                invocation.proceed()
            }.exceptionOrNull()
        }
        if (thrown != null) {
            throw thrown
        }
    }
}
