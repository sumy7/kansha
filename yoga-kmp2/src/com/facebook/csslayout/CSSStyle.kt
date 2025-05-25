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
 * The CSS style definition for a [CSSNode].
 */
class CSSStyle internal constructor() {
    var direction: CSSDirection? = null
    var flexDirection: CSSFlexDirection? = null
    var justifyContent: CSSJustify? = null
    var alignContent: CSSAlign? = null
    var alignItems: CSSAlign? = null
    var alignSelf: CSSAlign? = null
    var positionType: CSSPositionType? = null
    var flexWrap: CSSWrap? = null
    var flex: Float = 0f

    var margin: Spacing = Spacing()
    var padding: Spacing = Spacing()
    var border: Spacing = Spacing()

    var position: FloatArray = FloatArray(4)
    var dimensions: FloatArray = FloatArray(2)

    var minWidth: Float = CSSConstants.UNDEFINED
    var minHeight: Float = CSSConstants.UNDEFINED

    var maxWidth: Float = CSSConstants.UNDEFINED
    var maxHeight: Float = CSSConstants.UNDEFINED

    init {
        reset()
    }

    fun reset() {
        direction = CSSDirection.INHERIT
        flexDirection = CSSFlexDirection.COLUMN
        justifyContent = CSSJustify.FLEX_START
        alignContent = CSSAlign.FLEX_START
        alignItems = CSSAlign.STRETCH
        alignSelf = CSSAlign.AUTO
        positionType = CSSPositionType.RELATIVE
        flexWrap = CSSWrap.NOWRAP
        flex = 0f

        margin.reset()

        padding.reset()
        border.reset()

        position.fill(CSSConstants.UNDEFINED)
        dimensions.fill(CSSConstants.UNDEFINED)

        minWidth = CSSConstants.UNDEFINED
        minHeight = CSSConstants.UNDEFINED

        maxWidth = CSSConstants.UNDEFINED
        maxHeight = CSSConstants.UNDEFINED
    }
}
