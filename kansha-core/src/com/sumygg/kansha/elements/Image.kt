package com.sumygg.kansha.elements

class Image : Element() {
    var src: String? = null

    private fun add(element: Element): Nothing {
        throw UnsupportedOperationException("Image element cannot have children")
    }
}