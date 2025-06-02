package com.sumygg.kansha.elements

import com.sumygg.kansha.KanshaContext
import com.sumygg.kansha.builder.rect
import com.sumygg.kansha.handler.ImageData
import com.sumygg.kansha.handler.resolveImageSrc

class Image : Element() {
    var src: String? = null
    private var _imageData: ImageData? = null

    private fun add(element: Element): Nothing {
        throw UnsupportedOperationException("Image element cannot have children")
    }

    override suspend fun computeStyle() {
        super.computeStyle()
        // 解析 image src
        _imageData = resolveImageSrc(src)
        println("Image source: $src, Image data: $_imageData")
    }

    override fun render(appendable: Appendable, x: Float, y: Float, context: KanshaContext) {
        super.render(appendable, x, y, context)
        val dataUri = _imageData?.toDataUri()
        val left = x + cssNode.layoutX
        val top = y + cssNode.layoutY
        appendable.append(
            rect(
                this,
                "",
                left.toInt(),
                top.toInt(),
                cssNode.layoutWidth.toInt(),
                cssNode.layoutHeight.toInt(),
                src = dataUri,
                debug = context.debug
            )
        )
    }
}