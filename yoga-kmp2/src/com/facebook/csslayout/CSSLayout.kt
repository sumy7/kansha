/**
 * Copyright (c) 2014, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.csslayout

/**
 * Where the output of [LayoutEngine.layoutNode] will go in the CSSNode.
 */
open class CSSLayout {
    var position: FloatArray = FloatArray(4)
    var dimensions: FloatArray = FloatArray(2)
    var direction: com.facebook.csslayout.CSSDirection? = com.facebook.csslayout.CSSDirection.LTR

    /**
     * This should always get called before calling [LayoutEngine.layoutNode]
     */
    fun resetResult() {
        position.fill(0f)
        dimensions.fill(CSSConstants.UNDEFINED)
        direction = com.facebook.csslayout.CSSDirection.LTR
    }

    fun copy(layout: CSSLayout) {
        position[com.facebook.csslayout.CSSLayout.Companion.POSITION_LEFT] =
            layout.position[com.facebook.csslayout.CSSLayout.Companion.POSITION_LEFT]
        position[com.facebook.csslayout.CSSLayout.Companion.POSITION_TOP] =
            layout.position[com.facebook.csslayout.CSSLayout.Companion.POSITION_TOP]
        position[com.facebook.csslayout.CSSLayout.Companion.POSITION_RIGHT] =
            layout.position[com.facebook.csslayout.CSSLayout.Companion.POSITION_RIGHT]
        position[com.facebook.csslayout.CSSLayout.Companion.POSITION_BOTTOM] =
            layout.position[com.facebook.csslayout.CSSLayout.Companion.POSITION_BOTTOM]
        dimensions[com.facebook.csslayout.CSSLayout.Companion.DIMENSION_WIDTH] =
            layout.dimensions[com.facebook.csslayout.CSSLayout.Companion.DIMENSION_WIDTH]
        dimensions[com.facebook.csslayout.CSSLayout.Companion.DIMENSION_HEIGHT] =
            layout.dimensions[com.facebook.csslayout.CSSLayout.Companion.DIMENSION_HEIGHT]
        direction = layout.direction
    }

    override fun toString(): String {
        return "layout: {" +
                "left: " + position[com.facebook.csslayout.CSSLayout.Companion.POSITION_LEFT] + ", " +
                "top: " + position[com.facebook.csslayout.CSSLayout.Companion.POSITION_TOP] + ", " +
                "width: " + dimensions[com.facebook.csslayout.CSSLayout.Companion.DIMENSION_WIDTH] + ", " +
                "height: " + dimensions[com.facebook.csslayout.CSSLayout.Companion.DIMENSION_HEIGHT] + ", " +
                "direction: " + direction +
                "}"
    }

    companion object {
        const val POSITION_LEFT: Int = 0
        const val POSITION_TOP: Int = 1
        const val POSITION_RIGHT: Int = 2
        const val POSITION_BOTTOM: Int = 3

        const val DIMENSION_WIDTH: Int = 0
        const val DIMENSION_HEIGHT: Int = 1
    }
}
