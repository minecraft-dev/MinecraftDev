/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.RootProvider
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException

@Suppress("NonExtendableApiUsage")
class MockJdk(private val name: String, jar: VirtualFile, private val home: VirtualFile) :
    UserDataHolderBase(), Sdk, RootProvider {

    private val urls = arrayOf(jar.url)
    private val roots = arrayOf(jar)

    override fun getSdkType(): JavaSdk = JavaSdk.getInstance()
    override fun getName() = name
    override fun getVersionString() = name

    override fun getHomePath() = this.home.path
    override fun getHomeDirectory() = this.home

    override fun getRootProvider() = this

    override fun getSdkModificator() =
        throw IncorrectOperationException("Can't modify, MockJDK is read-only")

    override fun getSdkAdditionalData(): SdkAdditionalData? = null

    override fun clone() = throw CloneNotSupportedException()

    // Root provider

    override fun getUrls(rootType: OrderRootType): Array<String> =
        if (rootType == OrderRootType.CLASSES) urls else ArrayUtil.EMPTY_STRING_ARRAY

    override fun getFiles(rootType: OrderRootType): Array<VirtualFile> =
        if (rootType == OrderRootType.CLASSES) roots else VirtualFile.EMPTY_ARRAY

    override fun addRootSetChangedListener(listener: RootProvider.RootSetChangedListener) {}
    override fun addRootSetChangedListener(
        listener: RootProvider.RootSetChangedListener,
        parentDisposable: Disposable
    ) {
    }

    override fun removeRootSetChangedListener(listener: RootProvider.RootSetChangedListener) {}
}
