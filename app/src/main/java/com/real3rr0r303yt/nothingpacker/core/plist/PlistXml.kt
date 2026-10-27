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

package com.real3rr0r303yt.nothingpacker.core.plist

import com.real3rr0r303yt.nothingpacker.core.binary.MalformedFileException

/**
 * Tiny, dependency-free XML plist reader. It never resolves DTDs or external
 * entities, so a hostile Info.plist cannot trigger XXE or network access.
 */
internal object PlistXml {

    private sealed interface Token {
        data class Open(val name: String, val selfClosing: Boolean) : Token
        data class Close(val name: String) : Token
        data class Text(val text: String) : Token
    }

    fun parse(xml: String): Any? {
        val tokens = tokenize(xml).filterNot { it is Token.Text && it.text.isBlank() }.iterator()
        // Skip until <plist>, then read exactly one value.
        while (tokens.hasNext()) {
            val t = tokens.next()
            if (t is Token.Open && t.name == "plist") {
                if (t.selfClosing) return null
                return readValue(tokens.next(), tokens, 0)
            }
        }
        throw MalformedFileException("No <plist> element")
    }

    private fun readValue(first: Token, tokens: Iterator<Token>, depth: Int): Any? {
        if (depth > 128) throw MalformedFileException("Property list nested too deeply")
        val open = first as? Token.Open ?: throw MalformedFileException("Expected element")
        if (open.selfClosing) {
            return when (open.name) {
                "true" -> true
                "false" -> false
                "dict" -> emptyMap<String, Any?>()
                "array" -> emptyList<Any?>()
                "string", "key" -> ""
                "data" -> ByteArray(0)
                else -> null
            }
        }
        return when (open.name) {
            "dict" -> {
                val map = LinkedHashMap<String, Any?>()
                while (true) {
                    val t = tokens.next()
                    if (t is Token.Close) break
                    val key = readText(t, tokens, "key")
                    map[key] = readValue(tokens.next(), tokens, depth + 1)
                }
                map
            }
            "array" -> {
                val list = mutableListOf<Any?>()
                while (true) {
                    val t = tokens.next()
                    if (t is Token.Close) break
                    list += readValue(t, tokens, depth + 1)
                }
                list
            }
            "string" -> readTextBody(tokens)
            "integer" -> readTextBody(tokens).trim().toLong()
            "real" -> readTextBody(tokens).trim().toDouble()
            "data" -> java.util.Base64.getMimeDecoder().decode(readTextBody(tokens).filterNot { it.isWhitespace() })
            "date" -> PlistDate(
                java.time.Instant.parse(readTextBody(tokens).trim()).epochSecond - 978_307_200.0,
            )
            "true", "false" -> { tokens.next(); open.name == "true" }
            else -> { readTextBody(tokens); null }
        }
    }

    private fun readText(first: Token, tokens: Iterator<Token>, expected: String): String {
        val open = first as? Token.Open ?: throw MalformedFileException("Expected <$expected>")
        if (open.name != expected) throw MalformedFileException("Expected <$expected>, got <${open.name}>")
        return if (open.selfClosing) "" else readTextBody(tokens)
    }

    private fun readTextBody(tokens: Iterator<Token>): String {
        val sb = StringBuilder()
        while (true) {
            when (val t = tokens.next()) {
                is Token.Text -> sb.append(t.text)
                is Token.Close -> return sb.toString()
                is Token.Open -> throw MalformedFileException("Unexpected <${t.name}>")
            }
        }
    }

    private fun tokenize(xml: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        while (i < xml.length) {
            if (xml[i] != '<') {
                val end = xml.indexOf('<', i).let { if (it < 0) xml.length else it }
                out += Token.Text(unescape(xml.substring(i, end)))
                i = end
                continue
            }
            when {
                xml.startsWith("<!--", i) -> i = skipPast(xml, i, "-->")
                xml.startsWith("<![CDATA[", i) -> {
                    val end = xml.indexOf("]]>", i).takeIf { it >= 0 } ?: throw MalformedFileException("Unclosed CDATA")
                    out += Token.Text(xml.substring(i + 9, end))
                    i = end + 3
                }
                xml.startsWith("<?", i) -> i = skipPast(xml, i, "?>")
                xml.startsWith("<!", i) -> i = skipPast(xml, i, ">") // DOCTYPE – deliberately ignored
                else -> {
                    val end = xml.indexOf('>', i).takeIf { it >= 0 } ?: throw MalformedFileException("Unclosed tag")
                    val body = xml.substring(i + 1, end).trim()
                    out += when {
                        body.startsWith("/") -> Token.Close(body.substring(1).trim())
                        else -> {
                            val selfClosing = body.endsWith("/")
                            Token.Open(body.removeSuffix("/").trim().substringBefore(' ').substringBefore('\t'), selfClosing)
                        }
                    }
                    i = end + 1
                }
            }
        }
        return out
    }

    private fun skipPast(xml: String, from: Int, marker: String): Int =
        xml.indexOf(marker, from).takeIf { it >= 0 }?.plus(marker.length) ?: xml.length

    private fun unescape(s: String): String {
        if ('&' !in s) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '&') {
                val end = s.indexOf(';', i)
                if (end > i) {
                    val entity = s.substring(i + 1, end)
                    val decoded = when {
                        entity == "amp" -> "&"
                        entity == "lt" -> "<"
                        entity == "gt" -> ">"
                        entity == "quot" -> "\""
                        entity == "apos" -> "'"
                        entity.startsWith("#x") -> entity.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
                        entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) }
                        else -> null
                    }
                    if (decoded != null) {
                        sb.append(decoded)
                        i = end + 1
                        continue
                    }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
