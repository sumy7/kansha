package com.sumygg.kansha.elements

import com.facebook.csslayout.CSSNode
import com.facebook.csslayout.MeasureOutput
import com.sumygg.kansha.KanshaContext
import com.sumygg.kansha.builder.text

class Text : Element() {
    var fontFamily: String? = null
    var fontSize: Int? = null
    var color: String = "#000000" // 默认黑色

    var content: String? = null

    init {
//        this.cssNode.alignItems = CSSAlign.AUTO
//        this.cssNode.justifyContent = CSSJustify.FLEX_START
        this.cssNode.setMeasureFunction(
            object : CSSNode.MeasureFunction {
                override fun measure(
                    node: CSSNode,
                    width: Float,
                    measureOutput: MeasureOutput
                ) {
                    // TODO 不支持字体和换行，按照内容长度计算宽度
                    val text = content ?: ""
                    val widthPx = text.length * (fontSize ?: 16)
                    measureOutput.width = widthPx.toFloat()
                    measureOutput.height = (fontSize ?: 16).toFloat()
                }
            }
        )
    }

    /**
     * 添加子元素，Text 只能添加 Text 元素
     * TODO： 只支持单行字体
     */
//    fun add(text: Text): Text {
//        super.add(text)
//        return text
//    }

    override suspend fun computeStyle() {
        super.computeStyle()
    }

    override fun render(appendable: Appendable, x: Float, y: Float, context: KanshaContext) {
        super.render(appendable, x, y, context)
        appendable.append(
            text(
                node = this,
                id = "",
                left = (x + this.cssNode.layoutX).toInt(),
                top = (y + this.cssNode.layoutY).toInt(),
                width = this.cssNode.layoutWidth.toInt(),
                height = this.cssNode.layoutHeight.toInt(),
                debug = context.debug
            )
        )

    }

}