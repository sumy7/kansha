package com.sumygg.kansha.elements

import com.sumygg.kansha.KanshaContext
import com.sumygg.kansha.builder.rect

class View : Element() {

    override fun render(appendable: Appendable, context: KanshaContext) {
        super.render(appendable, context)
        appendable.append(
            rect(
                this,
                "",
                cssNode.layoutX.toInt(),
                cssNode.layoutY.toInt(),
                cssNode.layoutWidth.toInt(),
                cssNode.layoutHeight.toInt(),
                debug = context.debug
            )
        )
        if (hasChildren()) {
            children.forEach { child ->
                child.render(appendable, context)
            }
        }
    }

}