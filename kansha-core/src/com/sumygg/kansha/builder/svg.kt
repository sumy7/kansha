package com.sumygg.kansha.builder

import com.sumygg.kansha.buildXMLString

/**
 * 构造一个 SVG 字符串，包括宽度、高度和内容。
 *
 * @param width 宽度
 * @param height 高度
 * @param content SVG 内容
 * @return 构造好的 SVG 字符串
 */
fun svg(width: Int, height: Int, content: String): String {
    return buildXMLString(
        "svg",
        mapOf(
            "xmlns" to "http://www.w3.org/2000/svg",
            "width" to width.toString(),
            "height" to height.toString(),
            "viewBox" to "0 0 $width $height"
        ),
        content
    )
}