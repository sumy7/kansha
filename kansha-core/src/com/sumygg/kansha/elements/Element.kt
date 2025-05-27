package com.sumygg.kansha.elements

import com.facebook.csslayout.CSSNode
import com.sumygg.kansha.KanshaContext

abstract class Element {
    // yoga 布局元素
    val cssNode = CSSNode()

    // Element 的子元素
    val children = arrayListOf<Element>()

    // --- 通用样式定义 ---
    var width
        set(value) {
            cssNode.styleWidth = value.toFloat()
        }
        get() = cssNode.styleWidth.toInt()
    var height
        set(value) {
            cssNode.styleHeight = value.toFloat()
        }
        get() = cssNode.styleHeight.toInt()
    var borderWidth = 1
    var borderColor = "#000000" // 默认黑色边框
    var opacity = 1.0f
    var backgroundColor: String? = null
    var flex
        set(value) {
            cssNode.flex = value
        }
        get() = cssNode.flex
    var flexDirection
        set(value) {
            cssNode.flexDirection = value
        }
        get() = cssNode.flexDirection

    /**
     * 添加子元素
     */
    fun <E : Element> add(element: E) {
        children.add(element)
        cssNode.addChildAt(element.cssNode, cssNode.childCount)
    }

    /**
     * 是否存在子元素
     */
    fun hasChildren(): Boolean = children.isNotEmpty()

    /**
     * 渲染元素到 svg
     */
    open fun render(appendable: Appendable, x: Float, y: Float, context: KanshaContext) {}

}