package com.sumygg.kansha

import io.ktor.util.escapeHTML

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
 * 将字符串转换为 HTML 转义字符。
 */
fun escapeHTML(str: String): String {
    return str.escapeHTML()
}