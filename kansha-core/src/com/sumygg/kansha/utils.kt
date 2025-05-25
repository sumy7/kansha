package com.sumygg.kansha

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