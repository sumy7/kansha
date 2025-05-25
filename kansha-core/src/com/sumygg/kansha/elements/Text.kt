package com.sumygg.kansha.elements

class Text : Element() {
    var fontFamily: String? = null
    var fontSize: Int? = null

    /**
     * 添加子元素，Text 只能添加 Text 元素
     */
    fun add(text: Text): Text {
        super.add(text)
        return text
    }

}