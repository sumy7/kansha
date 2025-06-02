package com.sumygg.kansha

import com.facebook.csslayout.*
import com.sumygg.kansha.builder.svg
import com.sumygg.kansha.elements.Element

class KanshaContext {

    var debug = false
    var width: Int = -1
    var height: Int = -1

}

suspend fun kansha(block: KanshaContext.() -> Element): String {
    val context = KanshaContext()
    val element = context.block()

    val root = CSSNode()
    if (context.width >= 0) {
        root.styleWidth = context.width.toFloat()
    }
    if (context.height >= 0) {
        root.styleHeight = context.height.toFloat()
    }
    root.flexDirection = CSSFlexDirection.ROW
    root.setWrap(CSSWrap.WRAP)
    root.alignItems = CSSAlign.AUTO
    root.justifyContent = CSSJustify.FLEX_START
    root.addChildAt(element.cssNode, 0)

    element.computeStyle()

    val layoutContext = CSSLayoutContext()
    root.calculateLayout(layoutContext)

    val computedWidth = root.layoutWidth
    val computedHeight = root.layoutHeight

    return svg(
        width = computedWidth.toInt(),
        height = computedHeight.toInt(),
        content = buildString {
            element.render(this, 0f, 0f, context)
        }
    )
}