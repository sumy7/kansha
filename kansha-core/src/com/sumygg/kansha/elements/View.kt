package com.sumygg.kansha.elements

import com.sumygg.kansha.KanshaContext
import com.sumygg.kansha.builder.rect

class View : Element() {

    override suspend fun computeStyle() {
        super.computeStyle()
        if (hasChildren()) {
            children.forEach { child ->
                child.computeStyle()
            }
        }
    }

    override fun render(appendable: Appendable, x: Float, y: Float, context: KanshaContext) {
        super.render(appendable, x, y, context)
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
                debug = context.debug
            )
        )
        if (hasChildren()) {
            children.forEach { child ->
                child.render(appendable, left, top, context)
            }
        }
    }

}