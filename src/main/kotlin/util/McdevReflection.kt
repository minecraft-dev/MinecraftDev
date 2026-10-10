/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.util

import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase
import com.intellij.collaboration.auth.AccountManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.psi.PsiClass
import git4idea.remote.hosting.http.SilentHostedGitHttpAuthDataProviderBase
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.file.Path

/**
 * All reflective access to IntelliJ internals goes through this object, so that the existence of every member can be
 * verified in one place by `McdevReflectionTest`.
 */
object McdevReflection {
    @JvmStatic
    val CLASS_INJECTION_RESULT: Class<*> =
        Class.forName("com.intellij.psi.impl.source.tree.injected.InjectionResult")

    @JvmStatic
    val CLASS_INJECTION_REGISTRAR_IMPL: Class<*> =
        Class.forName("com.intellij.psi.impl.source.tree.injected.InjectionRegistrarImpl")

    @JvmStatic
    val METHOD_INJECTION_REGISTRAR_IMPL_ADD_TO_RESULTS: Method =
        CLASS_INJECTION_REGISTRAR_IMPL.getDeclaredMethodEx("addToResults", Void.TYPE, CLASS_INJECTION_RESULT)

    @JvmStatic
    val METHOD_INJECTION_REGISTRAR_IMPL_GET_INJECTED_RESULT: Method =
        CLASS_INJECTION_REGISTRAR_IMPL.getDeclaredMethodEx("getInjectedResult", CLASS_INJECTION_RESULT)

    @JvmStatic
    val METHOD_GENERATE_MEMBERS_HANDLER_BASE_DO_GENERATE: Method =
        GenerateMembersHandlerBase::class.java.getDeclaredMethodEx(
            "doGenerate",
            Void.TYPE,
            Project::class.java,
            Editor::class.java,
            PsiClass::class.java,
            Array<ClassMember>::class.java,
        )

    @JvmStatic
    val FIELD_PLUGIN_DOWNLOADER_MY_FILE: Field =
        PluginDownloader::class.java.getDeclaredFieldEx("myFile", Path::class.java)

    /**
     * Members from the Git4Idea plugin. Kept in a separate object so that they are only initialized when needed, and a
     * missing Git4Idea plugin doesn't break the rest of [McdevReflection].
     */
    object Git4Idea {
        @JvmStatic
        val METHOD_SILENT_HOSTED_GIT_HTTP_AUTH_DATA_PROVIDER_BASE_GET_PROVIDER_ID: Method =
            SilentHostedGitHttpAuthDataProviderBase::class.java
                .getDeclaredMethodEx("getProviderId", String::class.java)

        @JvmStatic
        val METHOD_SILENT_HOSTED_GIT_HTTP_AUTH_DATA_PROVIDER_BASE_GET_ACCOUNT_MANAGER: Method =
            SilentHostedGitHttpAuthDataProviderBase::class.java
                .getDeclaredMethodEx("getAccountManager", AccountManager::class.java)

        fun initialize() {
            // load the class, used for testing
        }
    }

    /**
     * Like [Class.getDeclaredField], but makes the field accessible and checks that it has the expected [type].
     */
    private fun Class<*>.getDeclaredFieldEx(name: String, type: Class<*>): Field {
        val field = getDeclaredField(name)
        check(field.type == type) {
            "Field $field has type ${field.type.name}, expected ${type.name}"
        }
        field.isAccessible = true
        return field
    }

    /**
     * Like [Class.getDeclaredMethod], but makes the method accessible and checks that it has the expected [returnType].
     */
    private fun Class<*>.getDeclaredMethodEx(name: String, returnType: Class<*>, vararg params: Class<*>): Method {
        val method = getDeclaredMethod(name, *params)
        check(method.returnType == returnType) {
            "Method $method has return type ${method.returnType.name}, expected ${returnType.name}"
        }
        method.isAccessible = true
        return method
    }

    /**
     * Like [Method.invoke], but rethrows the exception thrown by the invoked method rather than wrapping it in an
     * [InvocationTargetException].
     */
    fun Method.invokeUnwrap(obj: Any?, vararg args: Any?): Any? {
        try {
            return invoke(obj, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    fun initialize() {
        // load the class, used for testing
    }
}
