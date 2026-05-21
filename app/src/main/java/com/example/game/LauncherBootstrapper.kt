package com.example.game

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * PHASE 3: NATIVE JVM BOOTSTRAPPER
 * This class handles the construction of the massive classpath and JVM arguments.
 */
class LauncherBootstrapper(private val context: Context) {

    fun buildLaunchCommand(
        versionId: String,
        assetsDir: File,
        gameDir: File,
        libsDir: File,
        ramAllocation: String = "2G",
        playerName: String = "Player",
        javaPath: String = "/data/data/com.aistudio.cslauncher.vkmzqp/files/jdk-21/bin/java"
    ): List<String> {
        val classpath = buildClasspath(libsDir, versionId)
        val mainClass = "net.minecraft.client.main.Main"
        
        return mutableListOf<String>().apply {
            add(javaPath) // Use the provided Java binary path
            add("-Xmx$ramAllocation")
            add("-Xms$ramAllocation")
            add("-Djava.library.path=${libsDir.path}/natives")
            add("-cp")
            add(classpath)
            add(mainClass)
            
            // Minecraft Arguments
            add("--username")
            add(playerName)
            add("--version")
            add(versionId)
            add("--gameDir")
            add(gameDir.path)
            add("--assetsDir")
            add(assetsDir.path)
            add("--assetIndex")
            add(versionId) // Usually derived from version.json
            add("--uuid")
            add("00000000-0000-0000-0000-000000000000")
            add("--accessToken")
            add("null")
            add("--userType")
            add("legacy")
            add("--versionType")
            add("release")
        }
    }

    private fun buildClasspath(libsDir: File, versionId: String): String {
        val libFiles = libsDir.listFiles()?.filter { it.extension == "jar" } ?: emptyList()
        val clientJar = File(libsDir, "$versionId.jar")
        
        return (libFiles + clientJar).joinToString(File.pathSeparator) { it.path }
    }
}
