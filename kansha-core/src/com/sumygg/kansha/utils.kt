package com.sumygg.kansha

import io.ktor.util.*

/**
 * 构造一个 XML 字符串，包括标签名、属性和子元素。
 */
fun buildXMLString(
    type: String,
    attrs: Map<String, String>,
    children: String? = null
): String {
    val attrsString = attrs.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }
    return if (children != null) {
        "<$type $attrsString>$children</$type>"
    } else {
        "<$type $attrsString />"
    }
}

/**
 * 解析 SVG 的 viewBox 属性，返回一个整数列表。
 * 格式为 "minX minY width height"，例如 "0 0 100 100"。
 */
fun parseViewBox(viewBox: String?): List<Int>? {
    return viewBox?.split(Regex("[, ]"))?.map { it.toInt() }
}

/**
 * 将字符串转换为 HTML 转义字符。
 */
fun escapeHTML(str: String): String {
    return str.escapeHTML()
}

/**
 * 将 ByteArray 中的 4 字节转换为 UInt32。
 */
fun ByteArray.getUInt32(offset: Int = 0, littleEndian: Boolean = false): UInt {
    require(size >= offset + 4) { "ByteArray too small for UInt32 at offset $offset" }
    return if (littleEndian) {
        (this[offset].toUInt() and 0xFFu) or
                ((this[offset + 1].toUInt() and 0xFFu) shl 8) or
                ((this[offset + 2].toUInt() and 0xFFu) shl 16) or
                ((this[offset + 3].toUInt() and 0xFFu) shl 24)
    } else {
        (this[offset + 3].toUInt() and 0xFFu) or
                ((this[offset + 2].toUInt() and 0xFFu) shl 8) or
                ((this[offset + 1].toUInt() and 0xFFu) shl 16) or
                ((this[offset].toUInt() and 0xFFu) shl 24)
    }
}

/**
 * 将 ByteArray 中的 2 字节转换为 UInt16。
 */
fun ByteArray.getUInt16(offset: Int = 0, littleEndian: Boolean = false): UInt {
    require(size >= offset + 2) { "ByteArray too small for UInt16 at offset $offset" }
    return if (littleEndian) {
        (this[offset].toUInt() and 0xFFu) or
                ((this[offset + 1].toUInt() and 0xFFu) shl 8)
    } else {
        (this[offset + 1].toUInt() and 0xFFu) or
                ((this[offset].toUInt() and 0xFFu) shl 8)
    }
}

/**
 * 将 ByteArray 中的 1 字节转换为 UInt8。
 */
fun ByteArray.getUInt8(offset: Int = 0): UInt {
    require(size >= offset + 1) { "ByteArray too small for UInt8 at offset $offset" }
    return this[offset].toUInt() and 0xFFu
}
