package org.jetbrains.gradle.plugins.upx

import org.gradle.internal.os.OperatingSystem

@Suppress("EnumEntryName")
enum class UpxSupportedOperatingSystems(val fileSuffix: String, val extension: String = "tar.xz") {

    WINDOWS_x86("win32", "zip"),
    WINDOWS_x64("win64", "zip"),
    LINUX_x64("amd64_linux"),
    LINUX_ARM64("arm64_linux"),
    LINUX_ARM("amd_linux"),
    LINUX_x86("i386_linux");

    companion object {
        fun current(): UpxSupportedOperatingSystems? {
            val os = OperatingSystem.current()
            val is64 = "64" in System.getProperty("os.arch")
            return when {
                os.isWindows -> if (is64) WINDOWS_x64 else WINDOWS_x86
                os.isLinux -> {
                    val isArm = "arm" in System.getProperty("os.arch")
                    when {
                        is64 && isArm -> LINUX_ARM64
                        !is64 && isArm -> LINUX_ARM
                        is64 -> LINUX_x64
                        else -> LINUX_x86
                    }
                }
                else -> null
            }
        }
    }
}