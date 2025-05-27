package com.sumygg.kansha.builder

import com.sumygg.kansha.buildXMLString
import com.sumygg.kansha.elements.Text
import com.sumygg.kansha.escapeHTML

fun text(
    node: Text,
    id: String,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    debug: Boolean = false
): String {
    val extra = StringBuilder()
    if (debug) {
        extra.append(
            buildXMLString(
                "rect",
                mapOf(
                    "x" to left.toString(),
                    "y" to top.toString(),
                    "width" to width.toString(),
                    "height" to height.toString(),
                    "fill" to "none",
                    "stroke" to "#575eff",
                    "stroke-width" to "1"
                )
            )
        )
    }

    return buildString {
        append(
            buildXMLString(
                "text",
                mapOf(
                    "id" to id,
                    "x" to left.toString(),
                    "y" to top.toString(),
                    "width" to width.toString(),
                    "height" to height.toString(),
                    // TODO：当前 Yoga 引擎不支持 flex baseline 对齐
                    "alignment-baseline" to "hanging",
                    "font-family" to (node.fontFamily ?: "sans-serif"),
                    "font-size" to (node.fontSize ?: 16).toString(),
                    "fill" to node.color,
                    "opacity" to node.opacity.toString()
                ),
                escapeHTML(node.content ?: "")
            )
        )
        append(extra)
    }
}