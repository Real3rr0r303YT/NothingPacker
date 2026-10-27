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

import com.real3rr0r303yt.nothingpacker.core.axml.ANDROID_NS
import com.real3rr0r303yt.nothingpacker.core.axml.AxmlParser
import com.real3rr0r303yt.nothingpacker.core.binary.MalformedFileException
import com.real3rr0r303yt.nothingpacker.core.image.CgbiPng
import com.real3rr0r303yt.nothingpacker.core.macho.MachO
import com.real3rr0r303yt.nothingpacker.core.plist.Plist
import com.real3rr0r303yt.nothingpacker.core.plist.PlistDate
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.Inflater

class ParsersTest {

    @Test
    fun `binary plist decodes all value types`() {
        val p = Plist.parseDict(Fixtures.BINARY_INFO_PLIST)
        assertEquals("Dot Demo", p["CFBundleDisplayName"])
        assertEquals("Grüße ✓", p["Unicode"])
        assertEquals(1L shl 40, p["Big"])
        assertEquals(0.5, p["Ratio"])
        assertEquals(false, p["Flag"])
        assertArrayEquals(byteArrayOf(0, 1, 2), p["Blob"] as ByteArray)
        assertEquals(listOf(1L, 2L), p["UIDeviceFamily"])
        assertTrue(p["Built"] is PlistDate)
        @Suppress("UNCHECKED_CAST")
        val schemes = ((p["CFBundleURLTypes"] as List<Map<String, Any?>>)[0]["CFBundleURLSchemes"]) as List<*>
        assertEquals(listOf("dotdemo"), schemes)
    }

    @Test
    fun `xml plist round-trips`() {
        val original = Plist.parseDict(Fixtures.BINARY_INFO_PLIST)
        val reparsed = Plist.parseDict(Plist.toXml(original).toByteArray())
        assertEquals(original.keys, reparsed.keys)
        assertEquals(original["Unicode"], reparsed["Unicode"])
        assertEquals(original["CFBundleIcons"], reparsed["CFBundleIcons"])
        assertEquals((original["Built"] as PlistDate).secondsSince2001.toLong(), (reparsed["Built"] as PlistDate).secondsSince2001.toLong())
    }

    @Test
    fun `xml plist ignores doctype entities`() {
        val xml = """
            <?xml version="1.0"?>
            <!DOCTYPE plist [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
            <plist version="1.0"><dict><key>A</key><string>&xxe;&amp;&#x41;</string><key>B</key><true/></dict></plist>
        """.trimIndent()
        val p = Plist.parseDict(xml.toByteArray())
        assertEquals("&xxe;&A", p["A"])
        assertEquals(true, p["B"])
    }

    @Test(expected = MalformedFileException::class)
    fun `truncated binary plist is rejected`() {
        Plist.parse(Fixtures.BINARY_INFO_PLIST.copyOf(60))
    }

    @Test
    fun `axml manifest decodes`() {
        val root = AxmlParser.parse(Fixtures.manifest())
        assertEquals("manifest", root.name)
        assertEquals("com.example.dots", root.value("package", null))
        assertEquals("42", root.value("versionCode"))
        val app = root.child("application")!!
        assertEquals("@0x7f0d0000", app.value("icon"))
        assertTrue(app.attr("icon")!!.isReference)
        assertEquals("true", app.value("debuggable"))
        assertEquals(ANDROID_NS, app.attr("label")!!.namespace)
        val xml = AxmlParser.toXmlString(root)
        assertTrue(xml.contains("xmlns:android=\"$ANDROID_NS\""))
        assertTrue(xml.contains("android:name=\".MainActivity\""))
    }

    @Test(expected = MalformedFileException::class)
    fun `garbage axml is rejected`() {
        AxmlParser.parse(byteArrayOf(3, 0, 8, 0, 0x7f, 0x7f, 0x7f, 0x7f, 1, 0, 28, 0))
    }

    @Test
    fun `cgbi png becomes standard png`() {
        assertTrue(CgbiPng.isCgbi(Fixtures.CGBI_PNG))
        val png = CgbiPng.normalize(Fixtures.CGBI_PNG)!!
        assertFalse(CgbiPng.isCgbi(png))
        // Decode IDAT (zlib, filter 0) and compare straight RGBA pixels.
        var p = 8
        var idat = ByteArray(0)
        while (p < png.size) {
            val len = java.nio.ByteBuffer.wrap(png, p, 4).int
            val type = String(png, p + 4, 4)
            if (type == "IDAT") idat = png.copyOfRange(p + 8, p + 8 + len)
            p += 12 + len
        }
        val raw = ByteArray(2 * (1 + 8))
        Inflater().apply { setInput(idat); inflate(raw); end() }
        val pixels = raw.filterIndexed { i, _ -> i % 9 != 0 }.map { it.toInt() and 0xFF }
        val expected = listOf(255, 0, 0, 255, 0, 255, 0, 128, 0, 0, 0, 0, 200, 100, 50, 255)
        expected.zip(pixels).forEach { (e, a) -> assertTrue("expected $e got $a", Math.abs(e - a) <= 2) }
    }

    @Test
    fun `mach-o header reports arch and encryption`() {
        val info = MachO.inspect(Fixtures.ENCRYPTED_MACHO)!!
        assertEquals(listOf("arm64"), info.architectures)
        assertEquals(true, info.encrypted)
    }
}
