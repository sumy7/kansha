package com.sumygg.kansha.builder

import com.sumygg.kansha.buildXMLString
import com.sumygg.kansha.elements.Element

fun rect(
    node: Element,
    id: String,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    src: String? = null,
    debug: Boolean = false,
): String {
    val isImage = !src.isNullOrBlank()

    val opacity = node.opacity
    val fills = mutableListOf<String>()
    val shape = StringBuilder()
    val extra = StringBuilder()

    if (node.backgroundColor != null) {
        fills.add(node.backgroundColor!!)
    }

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
                    "stroke" to "#ff5757",
                    "stroke-width" to "1",
                )
            )
        )
    }

    fills.forEach { fill ->
        shape.append(
            buildXMLString(
                "rect",
                mapOf(
                    "x" to left.toString(),
                    "y" to top.toString(),
                    "width" to width.toString(),
                    "height" to height.toString(),
                    "fill" to fill,
                )
            )
        )
    }

    if (isImage) {
        extra.append(
            buildXMLString(
                "image",
                mapOf(
                    "x" to left.toString(),
                    "y" to top.toString(),
                    "width" to width.toString(),
                    "height" to height.toString(),
                    "href" to src,
                )
            )
        )
    }

    shape.append(
        border(
            node,
            left,
            top,
            width,
            height,
        )
    )


    return if (opacity < 1.0f) {
        buildXMLString(
            "g", mapOf(
                "opacity" to opacity.toString(),
            ), shape.toString()
        )
    } else {
        shape.toString()
    } + extra.toString()
}