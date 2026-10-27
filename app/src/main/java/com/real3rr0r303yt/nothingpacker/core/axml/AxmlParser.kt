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

package com.real3rr0r303yt.nothingpacker.core.axml

import com.real3rr0r303yt.nothingpacker.core.binary.ByteReader
import com.real3rr0r303yt.nothingpacker.core.binary.MalformedFileException

const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

data class XmlAttribute(
    val namespace: String?,
    val name: String,
    val value: String,
    /** Raw typed value, e.g. a resource id for references. */
    val type: Int = 0,
    val data: Int = 0,
) {
    val isReference: Boolean get() = type == AxmlParser.TYPE_REFERENCE
}

class XmlElement(
    val name: String,
    val namespace: String?,
    val attributes: List<XmlAttribute>,
) {
    val children = mutableListOf<XmlElement>()

    /** Namespace URI → prefix, collected while parsing (only filled on the root element). */
    var namespacePrefixes: Map<String, String> = emptyMap()

    fun attr(name: String, namespace: String? = ANDROID_NS): XmlAttribute? =
        attributes.firstOrNull { it.name == name && it.namespace == namespace }
            ?: if (namespace != null) null else attributes.firstOrNull { it.name == name }

    fun value(name: String, namespace: String? = ANDROID_NS): String? = attr(name, namespace)?.value

    fun child(name: String): XmlElement? = children.firstOrNull { it.name == name }

    fun children(name: String): List<XmlElement> = children.filter { it.name == name }
}

/**
 * Decoder for Android's binary XML (AXML) as found in compiled AndroidManifest.xml files.
 * Pure Kotlin, defensive against truncated or hostile input.
 */
object AxmlParser {

    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_XML_TYPE = 0x0003
    private const val RES_XML_START_NAMESPACE = 0x0100
    private const val RES_XML_END_NAMESPACE = 0x0101
    private const val RES_XML_START_ELEMENT = 0x0102
    private const val RES_XML_END_ELEMENT = 0x0103
    private const val RES_XML_RESOURCE_MAP = 0x0180

    const val TYPE_NULL = 0x00
    const val TYPE_REFERENCE = 0x01
    const val TYPE_ATTRIBUTE = 0x02
    const val TYPE_STRING = 0x03
    const val TYPE_FLOAT = 0x04
    const val TYPE_DIMENSION = 0x05
    const val TYPE_FRACTION = 0x06
    const val TYPE_INT_DEC = 0x10
    const val TYPE_INT_HEX = 0x11
    const val TYPE_INT_BOOLEAN = 0x12

    private const val MAX_DEPTH = 256

    fun isBinaryXml(bytes: ByteArray): Boolean =
        bytes.size >= 8 && bytes[0].toInt() == RES_XML_TYPE && bytes[1].toInt() == 0

    fun parse(bytes: ByteArray): XmlElement {
        val r = ByteReader(bytes)
        if (r.u16(0) != RES_XML_TYPE) throw MalformedFileException("Not a binary XML file")
        val fileEnd = minOf(r.u32(4), bytes.size.toLong()).toInt()
        var offset = r.u16(2)

        var strings = StringPool.EMPTY
        var resourceIds = IntArray(0)
        val stack = ArrayDeque<XmlElement>()
        var root: XmlElement? = null
        val namespaces = mutableMapOf<String, String>()

        while (offset + 8 <= fileEnd) {
            val type = r.u16(offset)
            val headerSize = r.u16(offset + 2)
            val chunkSize = r.u32(offset + 4).toInt()
            if (chunkSize < 8 || offset + chunkSize > fileEnd) throw MalformedFileException("Bad chunk at $offset")

            when (type) {
                RES_STRING_POOL_TYPE -> strings = StringPool.read(r, offset)
                RES_XML_RESOURCE_MAP -> {
                    val count = (chunkSize - headerSize) / 4
                    resourceIds = IntArray(count) { r.s32(offset + headerSize + it * 4) }
                }
                RES_XML_START_NAMESPACE -> {
                    val prefix = strings.getOrNull(r.s32(offset + 16))
                    val uri = strings.getOrNull(r.s32(offset + 20))
                    if (prefix != null && uri != null) namespaces[uri] = prefix
                }
                RES_XML_START_ELEMENT -> {
                    val ext = offset + headerSize
                    val ns = strings.getOrNull(r.s32(ext))
                    val name = strings.getOrNull(r.s32(ext + 4)) ?: "unknown"
                    val attrStart = r.u16(ext + 8)
                    val attrSize = r.u16(ext + 10).coerceAtLeast(20)
                    val attrCount = r.u16(ext + 12)
                    val attributes = (0 until attrCount).map { i ->
                        val a = ext + attrStart + i * attrSize
                        val nameIndex = r.s32(a + 4)
                        val rawIndex = r.s32(a + 8)
                        val dataType = r.u8(a + 15)
                        val data = r.s32(a + 16)
                        val attrName = strings.getOrNull(nameIndex).takeUnless { it.isNullOrEmpty() }
                            ?: resourceIds.getOrNull(nameIndex)?.let { AndroidAttributes.name(it) }
                            ?: "attr$nameIndex"
                        XmlAttribute(
                            namespace = strings.getOrNull(r.s32(a)),
                            name = attrName,
                            value = strings.getOrNull(rawIndex) ?: formatValue(dataType, data, strings),
                            type = dataType,
                            data = data,
                        )
                    }
                    val element = XmlElement(name, ns, attributes)
                    if (stack.size >= MAX_DEPTH) throw MalformedFileException("XML nested too deeply")
                    stack.lastOrNull()?.children?.add(element) ?: run { if (root == null) root = element }
                    stack.addLast(element)
                }
                RES_XML_END_ELEMENT -> stack.removeLastOrNull()
                RES_XML_END_NAMESPACE -> Unit
            }
            offset += chunkSize
        }
        return root?.also { it.namespacePrefixes = namespaces } ?: throw MalformedFileException("XML has no root element")
    }

    fun formatValue(type: Int, data: Int, strings: StringPool = StringPool.EMPTY): String = when (type) {
        TYPE_NULL -> ""
        TYPE_REFERENCE -> if (data == 0) "@null" else "@0x%08x".format(data)
        TYPE_ATTRIBUTE -> "?0x%08x".format(data)
        TYPE_STRING -> strings.getOrNull(data) ?: ""
        TYPE_FLOAT -> java.lang.Float.intBitsToFloat(data).toString()
        TYPE_DIMENSION -> formatComplex(data, DIMENSION_UNITS)
        TYPE_FRACTION -> formatComplex(data, FRACTION_UNITS, scale = 100f)
        TYPE_INT_HEX -> "0x%08x".format(data)
        TYPE_INT_BOOLEAN -> (data != 0).toString()
        in 0x1c..0x1f -> "#%08x".format(data)
        else -> data.toString()
    }

    private val DIMENSION_UNITS = arrayOf("px", "dp", "sp", "pt", "in", "mm")
    private val FRACTION_UNITS = arrayOf("%", "%p")
    private val RADIX_MULTS = floatArrayOf(1f / (1 shl 8), 1f / (1 shl 15), 1f / (1 shl 23), 1f / (1 shl 31))

    private fun formatComplex(data: Int, units: Array<String>, scale: Float = 1f): String {
        val mantissa = (data and 0xFFFFFF00.toInt()).toFloat()
        val value = mantissa * RADIX_MULTS[(data shr 4) and 0x3] * scale
        val unit = units.getOrElse(data and 0xF) { "" }
        val text = if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()
        return text + unit
    }

    /** Pretty-prints a decoded tree as regular XML text. */
    fun toXmlString(root: XmlElement): String {
        val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        val prefixes = root.namespacePrefixes
        fun qualified(ns: String?, name: String) = prefixes[ns]?.let { "$it:$name" } ?: name
        fun write(e: XmlElement, depth: Int, isRoot: Boolean) {
            val indent = "    ".repeat(depth)
            sb.append(indent).append('<').append(qualified(e.namespace, e.name))
            if (isRoot) prefixes.forEach { (uri, prefix) -> sb.append(" xmlns:").append(prefix).append("=\"").append(escape(uri)).append('"') }
            e.attributes.forEach { a ->
                sb.append('\n').append(indent).append("    ").append(qualified(a.namespace, a.name))
                    .append("=\"").append(escape(a.value)).append('"')
            }
            if (e.children.isEmpty()) {
                sb.append(" />\n")
            } else {
                sb.append(">\n")
                e.children.forEach { write(it, depth + 1, false) }
                sb.append(indent).append("</").append(qualified(e.namespace, e.name)).append(">\n")
            }
        }
        write(root, 0, true)
        return sb.toString()
    }

    fun escape(s: String): String = buildString(s.length) {
        s.forEach { c ->
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                else -> append(c)
            }
        }
    }
}
