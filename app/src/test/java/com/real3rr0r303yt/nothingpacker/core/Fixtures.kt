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

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Test fixtures. Binary plist and CgBI PNG were generated with Python's plistlib/zlib. */
object Fixtures {

    val BINARY_INFO_PLIST: ByteArray = Base64.getDecoder().decode(
        "YnBsaXN0MDDfEBMBAgMEBQYHCAkKCwwNDg8QERITFBUWFxgZHxggISYnKCkqKywtMFNCaWdUQmxvYlVCdWlsdF8QE0NGQnVuZGxlRGlzcGxheU5hbWVfEBJDRkJ1bmRsZUV4ZWN1dGFibGVdQ0ZCdW5kbGVJY29uc18QEkNGQnVuZGxlSWRlbnRpZmllclxDRkJ1bmRsZU5hbWVfEBpDRkJ1bmRsZVNob3J0VmVyc2lvblN0cmluZ18QEENGQnVuZGxlVVJMVHlwZXNfEA9DRkJ1bmRsZVZlcnNpb25fEBFEVFBsYXRmb3JtVmVyc2lvblRGbGFnXxAQTWluaW11bU9TVmVyc2lvbl8QGE5TQ2FtZXJhVXNhZ2VEZXNjcmlwdGlvbl8QHE5TTWljcm9waG9uZVVzYWdlRGVzY3JpcHRpb25VUmF0aW9eVUlEZXZpY2VGYW1pbHlXVW5pY29kZRMAAAEAAAAAAEMAAQIzQcWh2lKAAABYRG90IERlbW9XRG90RGVtb9EaG18QE0NGQnVuZGxlUHJpbWFyeUljb27RHB1fEBFDRkJ1bmRsZUljb25GaWxlc6EeXEFwcEljb242MHg2MF8QFGNvbS5leGFtcGxlLmRvdC1kZW1vUzIuMaEi0SMkXxASQ0ZCdW5kbGVVUkxTY2hlbWVzoSVXZG90ZGVtb1UyLjEuN1QxNy41CFQxNi4wXVNjYW4gUVIgY29kZXNbVm9pY2UgbWVtb3MjP+AAAAAAAACiLi8QARACZwBHAHIA/ADfAGUAICcTAAgAMQA1ADoAQABWAGsAeQCOAJsAuADLAN0A8QD2AQkBJAFDAUkBWAFgAWkBbQF2AX8BhwGKAaABowG3AbkBxgHdAeEB4wHmAfsB/QIFAgsCEAIRAhYCJAIwAjkCPAI+AkAAAAAAAAACAQAAAAAAAAAxAAAAAAAAAAAAAAAAAAACTw==",
    )

    /** 2×2 CgBI PNG: red, green@128, blue@0, (200,100,50) – premultiplied BGRA, Sub filter, raw deflate. */
    val CGBI_PNG: ByteArray = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAAEQ2dCSVAAIAIr1bN/AAAADUlIRFIAAAACAAAAAggGAAAAcrYNJAAAABNJREFUY2Rg+P+foYGxkZEBCIxSTvwHAIanpFQAAAAASUVORK5CYII=",
    )

    /** 64-bit arm64 Mach-O header with LC_ENCRYPTION_INFO_64 (cryptid = 1). */
    val ENCRYPTED_MACHO: ByteArray = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN).apply {
        putInt(0xFEEDFACF.toInt()); putInt(0x0100000C); putInt(0); putInt(2) // magic, cputype, subtype, filetype
        putInt(1); putInt(24); putInt(0); putInt(0) // ncmds, sizeofcmds, flags, reserved
        putInt(0x2C); putInt(24); putInt(0x4000); putInt(0x1000); putInt(1); putInt(0)
    }.array()

    sealed interface AttrValue
    data class Str(val value: String) : AttrValue
    data class IntVal(val value: Int) : AttrValue
    data class Ref(val id: Int) : AttrValue
    data class Bool(val value: Boolean) : AttrValue

    /** Minimal binary-XML (AXML) encoder for tests. */
    class Axml {
        private val strings = mutableListOf<String>()
        private val events = mutableListOf<ByteArray>()

        private fun idx(s: String?): Int = when (s) {
            null -> -1
            else -> strings.indexOf(s).takeIf { it >= 0 } ?: strings.run { add(s); size - 1 }
        }

        private fun chunk(type: Int, headerSize: Int, body: ByteBuffer.() -> Unit): ByteArray {
            val buf = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            buf.putShort(type.toShort()); buf.putShort(headerSize.toShort()); buf.putInt(0)
            buf.body()
            val size = buf.position()
            buf.putInt(4, size)
            return buf.array().copyOf(size)
        }

        fun startNamespace(prefix: String, uri: String) = apply {
            val p = idx(prefix); val u = idx(uri)
            events += chunk(0x0100, 16) { putInt(1); putInt(-1); putInt(p); putInt(u) }
        }

        fun start(name: String, vararg attrs: Triple<String?, String, AttrValue>) = apply {
            val n = idx(name)
            val encoded = attrs.map { (ns, attr, value) ->
                val nsIdx = idx(ns); val nameIdx = idx(attr)
                val (raw, type, data) = when (value) {
                    is Str -> idx(value.value).let { Triple(it, 0x03, it) }
                    is IntVal -> Triple(-1, 0x10, value.value)
                    is Ref -> Triple(-1, 0x01, value.id)
                    is Bool -> Triple(-1, 0x12, if (value.value) -1 else 0)
                }
                intArrayOf(nsIdx, nameIdx, raw, type, data)
            }
            events += chunk(0x0102, 16) {
                putInt(1); putInt(-1); putInt(-1); putInt(n)
                putShort(20); putShort(20); putShort(encoded.size.toShort()); putShort(0); putShort(0); putShort(0)
                encoded.forEach { a -> putInt(a[0]); putInt(a[1]); putInt(a[2]); putShort(8); put(0); put(a[3].toByte()); putInt(a[4]) }
            }
        }

        fun end(name: String) = apply {
            val n = idx(name)
            events += chunk(0x0103, 16) { putInt(1); putInt(-1); putInt(-1); putInt(n) }
        }

        fun build(): ByteArray {
            val data = ByteArrayOutputStream()
            val offsets = strings.map { s ->
                val offset = data.size()
                data.write(s.length and 0xFF); data.write(s.length shr 8)
                data.write(s.toByteArray(Charsets.UTF_16LE)); data.write(0); data.write(0)
                offset
            }
            while (data.size() % 4 != 0) data.write(0)
            val header = 28 + offsets.size * 4
            val pool = ByteBuffer.allocate(header + data.size()).order(ByteOrder.LITTLE_ENDIAN).apply {
                putShort(1); putShort(28); putInt(header + data.size()); putInt(strings.size); putInt(0); putInt(0)
                putInt(header); putInt(0)
                offsets.forEach { putInt(it) }
                put(data.toByteArray())
            }.array()
            val body = ByteArrayOutputStream().apply { write(pool); events.forEach { write(it) } }.toByteArray()
            return ByteBuffer.allocate(8 + body.size).order(ByteOrder.LITTLE_ENDIAN).apply {
                putShort(3); putShort(8); putInt(8 + body.size); put(body)
            }.array()
        }
    }

    const val ANDROID = "http://schemas.android.com/apk/res/android"

    fun manifest(): ByteArray = Axml()
        .startNamespace("android", ANDROID)
        .start(
            "manifest",
            Triple(ANDROID, "versionCode", IntVal(42)),
            Triple(ANDROID, "versionName", Str("1.4.2")),
            Triple(null, "package", Str("com.example.dots")),
        )
        .start("uses-sdk", Triple(ANDROID, "minSdkVersion", IntVal(26)), Triple(ANDROID, "targetSdkVersion", IntVal(35))).end("uses-sdk")
        .start("uses-permission", Triple(ANDROID, "name", Str("android.permission.CAMERA"))).end("uses-permission")
        .start("uses-permission", Triple(ANDROID, "name", Str("android.permission.VIBRATE"))).end("uses-permission")
        .start(
            "application",
            Triple(ANDROID, "label", Str("Dots")),
            Triple(ANDROID, "icon", Ref(0x7f0d0000)),
            Triple(ANDROID, "debuggable", Bool(true)),
        )
        .start("activity", Triple(ANDROID, "name", Str(".MainActivity")), Triple(ANDROID, "exported", Bool(true)))
        .start("intent-filter")
        .start("action", Triple(ANDROID, "name", Str("android.intent.action.MAIN"))).end("action")
        .start("category", Triple(ANDROID, "name", Str("android.intent.category.LAUNCHER"))).end("category")
        .end("intent-filter")
        .end("activity")
        .start("service", Triple(ANDROID, "name", Str("com.example.dots.SyncService"))).end("service")
        .end("application")
        .end("manifest")
        .build()

    fun zip(dir: File, name: String, entries: Map<String, ByteArray>): File = File(dir, name).also { file ->
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (path, bytes) ->
                zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry()
            }
        }
    }

    fun apk(dir: File): File = zip(
        dir, "dots.apk",
        linkedMapOf(
            "AndroidManifest.xml" to manifest(),
            "classes.dex" to "dex\n035".toByteArray(),
            "classes2.dex" to "dex\n035".toByteArray(),
            "lib/arm64-v8a/libdots.so" to ByteArray(16),
            "lib/x86_64/libdots.so" to ByteArray(16),
            "assets/config/settings.json" to "{\"dots\":true}".toByteArray(),
            "res/mipmap-xxhdpi/ic_launcher.png" to CGBI_PNG,
            "res/layout/main.xml" to ByteArray(8),
            "res/raw/beep.ogg" to ByteArray(4),
            "META-INF/CERT.RSA" to ByteArray(4),
        ),
    )

    fun ipa(dir: File): File = zip(
        dir, "dotdemo.ipa",
        linkedMapOf(
            "Payload/DotDemo.app/Info.plist" to BINARY_INFO_PLIST,
            "Payload/DotDemo.app/DotDemo" to ENCRYPTED_MACHO,
            "Payload/DotDemo.app/AppIcon60x60@2x.png" to CGBI_PNG,
            "Payload/DotDemo.app/AppIcon60x60@3x.png" to CGBI_PNG + ByteArray(10),
            "Payload/DotDemo.app/Assets.car" to ByteArray(32),
            "Payload/DotDemo.app/Base.lproj/Main.storyboardc/Info.plist" to ByteArray(4),
            "Payload/DotDemo.app/sounds/click.caf" to ByteArray(12),
            "Payload/DotDemo.app/Frameworks/Dots.framework/Dots" to ByteArray(12),
            "Payload/DotDemo.app/PlugIns/Share.appex/Share" to ByteArray(12),
            "Payload/DotDemo.app/_CodeSignature/CodeResources" to ByteArray(4),
            "Payload/DotDemo.app/../../evil.txt" to "nope".toByteArray(),
            "iTunesMetadata.plist" to ByteArray(4),
        ),
    )
}
