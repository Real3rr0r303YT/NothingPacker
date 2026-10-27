/*
 * NothingPacker – offline APK ⇄ IPA toolkit for Android
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.real3rr0r303yt.nothingpacker.core

import com.real3rr0r303yt.nothingpacker.core.analysis.PackageAnalyzer
import com.real3rr0r303yt.nothingpacker.core.analysis.sanitizePath
import com.real3rr0r303yt.nothingpacker.core.convert.Extractor
import com.real3rr0r303yt.nothingpacker.core.convert.StructureConverter
import com.real3rr0r303yt.nothingpacker.core.model.ComponentKind
import com.real3rr0r303yt.nothingpacker.core.model.FileCategory
import com.real3rr0r303yt.nothingpacker.core.model.PackageType
import com.real3rr0r303yt.nothingpacker.core.model.UnsupportedPackageException
import com.real3rr0r303yt.nothingpacker.core.model.Warning
import com.real3rr0r303yt.nothingpacker.core.plist.Plist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

class PackagesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> = buildMap {
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                put(e.name, zip.readBytes())
            }
        }
    }

    @Test
    fun `analyses apk`() {
        val r = PackageAnalyzer.analyze(Fixtures.apk(tmp.root))
        assertEquals(PackageType.APK, r.type)
        assertEquals("Dots", r.appName)
        assertEquals("com.example.dots", r.identifier)
        assertEquals("1.4.2", r.versionName)
        assertEquals("42", r.versionCode)
        assertEquals("API 26 · Android 8.0", r.minOs)
        assertEquals(listOf("android.permission.CAMERA", "android.permission.VIBRATE"), r.permissions.map { it.name })
        val launcher = r.components.first()
        assertEquals(ComponentKind.ACTIVITY, launcher.kind)
        assertEquals("com.example.dots.MainActivity", launcher.name)
        assertTrue("launcher" in launcher.flags)
        assertEquals(listOf("arm64-v8a", "x86_64"), r.architectures)
        assertEquals("res/mipmap-xxhdpi/ic_launcher.png", r.iconPath)
        assertEquals(2, r.stats[FileCategory.CODE]!!.count)
        assertEquals("JAR (v1)", r.signature)
        assertTrue(Warning.DEBUGGABLE in r.warnings)
    }

    @Test
    fun `analyses ipa`() {
        val file = Fixtures.ipa(tmp.root)
        val r = PackageAnalyzer.analyze(file)
        assertEquals(PackageType.IPA, r.type)
        assertEquals("Dot Demo", r.appName)
        assertEquals("com.example.dot-demo", r.identifier)
        assertEquals("iOS 16.0", r.minOs)
        assertEquals("Payload/DotDemo.app/", r.appRoot)
        assertEquals(listOf("NSCameraUsageDescription", "NSMicrophoneUsageDescription"), r.permissions.map { it.name })
        assertEquals("Scan QR codes", r.permissions[0].detail)
        assertEquals(listOf("arm64"), r.architectures)
        assertTrue(Warning.ENCRYPTED in r.warnings)
        assertEquals(setOf("dotdemo://", "Share.appex", "Dots.framework"), r.components.map { it.name }.toSet())
        assertEquals("Payload/DotDemo.app/AppIcon60x60@3x.png", r.iconPath)
        assertEquals(FileCategory.CODE, r.entries.first { it.path.endsWith("/DotDemo") }.category)
        assertEquals(FileCategory.RESOURCES, r.entries.first { it.path.endsWith("Assets.car") }.category)
        assertTrue(r.manifestText.contains("<key>CFBundleIdentifier</key>"))
        assertNotNull(PackageAnalyzer.readIcon(file, r))
    }

    @Test(expected = UnsupportedPackageException::class)
    fun `rejects other zips`() {
        PackageAnalyzer.analyze(Fixtures.zip(tmp.root, "x.zip", mapOf("hello.txt" to ByteArray(1))))
    }

    @Test(expected = UnsupportedPackageException::class)
    fun `rejects non zip`() {
        PackageAnalyzer.analyze(File(tmp.root, "x.apk").apply { writeText("not a zip") })
    }

    @Test
    fun `apk converts to ipa structure`() {
        val file = Fixtures.apk(tmp.root)
        val r = PackageAnalyzer.analyze(file)
        val out = ByteArrayOutputStream()
        val result = StructureConverter.convert(file, r, iconPng = byteArrayOf(1, 2, 3), out = out)
        assertEquals(PackageType.IPA, result.target)
        val files = unzip(out.toByteArray())
        val app = "Payload/Dots.app/"
        val info = Plist.parseDict(files.getValue(app + "Info.plist"))
        assertEquals("com.example.dots", info["CFBundleIdentifier"])
        assertEquals("42", info["CFBundleVersion"])
        assertTrue("NSCameraUsageDescription" in info)
        assertTrue(app + "AppIcon60x60@3x.png" in files)
        assertTrue(app + "assets/config/settings.json" in files)
        assertTrue(app + "res/raw/beep.ogg" in files)
        assertFalse("compiled XML is not portable", app + "res/layout/main.xml" in files)
        assertFalse("no code is ever copied", files.keys.any { it.endsWith(".dex") || it.endsWith(".so") })
        assertTrue("NothingPacker-README.txt" in files)
        assertEquals("Dots_1_4_2_ipa-structure.zip", StructureConverter.suggestedFileName(r))
    }

    @Test
    fun `ipa converts to apk structure`() {
        val file = Fixtures.ipa(tmp.root)
        val r = PackageAnalyzer.analyze(file)
        val out = ByteArrayOutputStream()
        StructureConverter.convert(file, r, iconPng = byteArrayOf(9), out = out)
        val files = unzip(out.toByteArray())
        val manifest = String(files.getValue("AndroidManifest.xml"))
        assertTrue(manifest.contains("package=\"com.example.dot_demo\""))
        assertTrue(manifest.contains("android:versionCode=\"20107\""))
        assertTrue(manifest.contains("android.permission.CAMERA"))
        assertTrue(manifest.contains("android.permission.RECORD_AUDIO"))
        assertTrue(String(files.getValue("res/values/strings.xml")).contains(">Dot Demo<"))
        assertTrue("res/mipmap-xxxhdpi/ic_launcher.png" in files)
        assertTrue("assets/sounds/click.caf" in files)
        assertTrue("assets/Assets.car" in files)
        assertTrue("ios/Info.plist" in files)
        assertFalse("no Mach-O code", files.keys.any { it.endsWith("/DotDemo") || it.contains("Frameworks") || it.contains("PlugIns") })
        assertFalse("no traversal", files.keys.any { it.contains("..") })
    }

    @Test
    fun `extracts selected categories safely`() {
        val file = Fixtures.ipa(tmp.root)
        val r = PackageAnalyzer.analyze(file)
        val target = tmp.newFolder("out")
        val result = Extractor.extract(file, r, setOf(FileCategory.METADATA, FileCategory.ASSETS), null, Extractor.directorySink(target))
        assertTrue(File(target, "metadata/Info.plist").readText().contains("Dot Demo"))
        assertTrue(File(target, "metadata/report.txt").exists())
        assertTrue(File(target, "files/Payload/DotDemo.app/sounds/click.caf").exists())
        assertFalse(File(target, "files/Payload/DotDemo.app/DotDemo").exists())
        assertFalse(File(tmp.root, "evil.txt").exists())
        assertTrue(result.files >= 3)
    }

    @Test
    fun `helpers`() {
        assertEquals("a/b/c.txt", sanitizePath("../a/./b//c.txt"))
        assertEquals("Payload/x.app/evil.txt", sanitizePath("Payload/x.app/../../evil.txt"))
        assertEquals("app.example", StructureConverter.toPackageName("example"))
        assertEquals("com._9lives.app", StructureConverter.toPackageName("com.9lives.app"))
        assertEquals(1234L, StructureConverter.versionCode("1234"))
        assertEquals(10203L, StructureConverter.versionCode("1.2.3"))
        assertEquals(1L, StructureConverter.versionCode(null))
    }
}
