package com.sumygg.kansha.builder

import com.sumygg.kansha.buildXMLString
import com.sumygg.kansha.elements.Element

fun border(
    node: Element,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
): String {
    return buildXMLString(
        "rect",
        mapOf(
            "x" to left.toString(),
            "y" to top.toString(),
            "width" to width.toString(),
            "height" to height.toString(),
            "stroke" to node.borderColor,
            "stroke-width" to node.borderWidth.toString(),
        )
    )
}