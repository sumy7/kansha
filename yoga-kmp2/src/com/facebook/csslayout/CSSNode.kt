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
 * A CSS Node. It has a style object you can manipulate at [.style]. After calling
 * [.calculateLayout], [.layout] will be filled with the results of the layout.
 */
class CSSNode {
    private enum class LayoutState {
        /**
         * Some property of this node or its children has changes and the current values in
         * [.layout] are not valid.
         */
        DIRTY,

        /**
         * This node has a new layout relative to the last time [.markLayoutSeen] was called.
         */
        HAS_NEW_LAYOUT,

        /**
         * [.layout] is valid for the node's properties and this layout has been marked as
         * having been seen.
         */
        UP_TO_DATE,
    }

    interface MeasureFunction {
        /**
         * Should measure the given node and put the result in the given MeasureOutput.
         *
         * NB: measure is NOT guaranteed to be threadsafe/re-entrant safe!
         */
        fun measure(node: CSSNode?, width: Float, measureOutput: MeasureOutput?)
    }

    // VisibleForTesting
    /*package*/
    val style: CSSStyle = CSSStyle()

    /*package*/
    val layout: CSSLayout = CSSLayout()

    /*package*/
    val lastLayout: CachedCSSLayout = CachedCSSLayout()

    var lineIndex: Int = 0

    /*package*/
    var nextAbsoluteChild: CSSNode? = null

    /*package*/
    var nextFlexChild: CSSNode? = null

    private var mChildren: ArrayList<CSSNode?>? = null

    var parent: CSSNode? = null
        private set

    private var mMeasureFunction: MeasureFunction? = null
    private var mLayoutState = LayoutState.DIRTY

    val childCount: Int
        get() = if (mChildren == null) 0 else mChildren!!.size

    fun getChildAt(i: Int): CSSNode {
        return mChildren!!.get(i)!!
    }

    fun addChildAt(child: CSSNode, i: Int) {
        check(child.parent == null) { "Child already has a parent, it must be removed first." }
        if (mChildren == null) {
            // 4 is kinda arbitrary, but the default of 10 seems really high for an average View.
            mChildren = ArrayList(4)
        }

        mChildren!!.add(i, child)
        child.parent = this
        dirty()
    }

    fun removeChildAt(i: Int): CSSNode {
        val removed: CSSNode = mChildren!!.removeAt(i)!!
        removed.parent = null
        dirty()
        return removed
    }

    /**
     * @return the index of the given child, or -1 if the child doesn't exist in this node.
     */
    fun indexOf(child: CSSNode?): Int {
        return mChildren!!.indexOf(child)
    }

    fun setMeasureFunction(measureFunction: MeasureFunction?) {
        if (mMeasureFunction !== measureFunction) {
            mMeasureFunction = measureFunction
            dirty()
        }
    }

    val isMeasureDefined: Boolean
        get() = mMeasureFunction != null

    /*package*/
    fun measure(measureOutput: MeasureOutput, width: Float): MeasureOutput {
        if (!this.isMeasureDefined) {
            throw RuntimeException("Measure function isn't defined!")
        }
        measureOutput.height = CSSConstants.UNDEFINED
        measureOutput.width = CSSConstants.UNDEFINED
        mMeasureFunction!!.measure(this, width, measureOutput)
        return measureOutput
    }

    /**
     * Performs the actual layout and saves the results in [.layout]
     */
    fun calculateLayout(layoutContext: CSSLayoutContext) {
        layout.resetResult()
        LayoutEngine.layoutNode(layoutContext, this, CSSConstants.UNDEFINED, null)
    }

    val isDirty: Boolean
        /**
         * See [LayoutState.DIRTY].
         */
        get() = mLayoutState == LayoutState.DIRTY

    /**
     * See [LayoutState.HAS_NEW_LAYOUT].
     */
    fun hasNewLayout(): Boolean {
        return mLayoutState == LayoutState.HAS_NEW_LAYOUT
    }

    protected fun dirty() {
        if (mLayoutState == LayoutState.DIRTY) {
            return
        } else check(mLayoutState != LayoutState.HAS_NEW_LAYOUT) { "Previous layout was ignored! markLayoutSeen() never called" }

        mLayoutState = LayoutState.DIRTY

        if (this.parent != null) {
            parent!!.dirty()
        }
    }

    /*package*/
    fun markHasNewLayout() {
        mLayoutState = LayoutState.HAS_NEW_LAYOUT
    }

    /**
     * Tells the node that the current values in [.layout] have been seen. Subsequent calls
     * to [.hasNewLayout] will return false until this node is laid out with new parameters.
     * You must call this each time the layout is generated if the node has a new layout.
     */
    fun markLayoutSeen() {
        check(hasNewLayout()) { "Expected node to have a new layout to be seen!" }

        mLayoutState = LayoutState.UP_TO_DATE
    }

    private fun toStringWithIndentation(result: StringBuilder, level: Int) {
        // Spaces and tabs are dropped by IntelliJ logcat integration, so rely on __ instead.
        val indentation = StringBuilder()
        for (i in 0..<level) {
            indentation.append("__")
        }

        result.append(indentation.toString())
        result.append(layout.toString())

        if (this.childCount == 0) {
            return
        }

        result.append(", children: [\n")
        for (i in 0..<this.childCount) {
            getChildAt(i).toStringWithIndentation(result, level + 1)
            result.append("\n")
        }
        result.append(indentation.toString() + "]")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        this.toStringWithIndentation(sb, 0)
        return sb.toString()
    }

    protected fun valuesEqual(f1: Float, f2: Float): Boolean {
        return FloatUtil.floatsEqual(f1, f2)
    }

    val styleDirection: CSSDirection?
        /**
         * Get this node's direction, as defined in the style.
         */
        get() = style.direction

    fun setDirection(direction: CSSDirection?) {
        if (style.direction !== direction) {
            style.direction = direction
            dirty()
        }
    }

    var flexDirection: CSSFlexDirection?
        /**
         * Get this node's flex direction, as defined by style.
         */
        get() = style.flexDirection
        set(flexDirection) {
            if (style.flexDirection !== flexDirection) {
                style.flexDirection = flexDirection
                dirty()
            }
        }

    var justifyContent: CSSJustify?
        /**
         * Get this node's justify content, as defined by style.
         */
        get() = style.justifyContent
        set(justifyContent) {
            if (style.justifyContent !== justifyContent) {
                style.justifyContent = justifyContent
                dirty()
            }
        }

    var alignItems: CSSAlign?
        /**
         * Get this node's align items, as defined by style.
         */
        get() = style.alignItems
        set(alignItems) {
            if (style.alignItems !== alignItems) {
                style.alignItems = alignItems
                dirty()
            }
        }

    var alignSelf: CSSAlign?
        /**
         * Get this node's align items, as defined by style.
         */
        get() = style.alignSelf
        set(alignSelf) {
            if (style.alignSelf !== alignSelf) {
                style.alignSelf = alignSelf
                dirty()
            }
        }

    val positionType: CSSPositionType?
        /**
         * Get this node's position type, as defined by style.
         */
        get() = style.positionType

    fun setPositionType(positionType: CSSPositionType?) {
        if (style.positionType !== positionType) {
            style.positionType = positionType
            dirty()
        }
    }

    fun setWrap(flexWrap: CSSWrap?) {
        if (style.flexWrap !== flexWrap) {
            style.flexWrap = flexWrap
            dirty()
        }
    }

    var flex: Float
        /**
         * Get this node's flex, as defined by style.
         */
        get() = style.flex
        set(flex) {
            if (!valuesEqual(style.flex, flex)) {
                style.flex = flex
                dirty()
            }
        }

    val margin: Spacing
        /**
         * Get this node's margin, as defined by style + default margin.
         */
        get() = style.margin

    fun setMargin(spacingType: Int, margin: Float) {
        if (style.margin.set(spacingType, margin)) {
            dirty()
        }
    }

    val padding: Spacing
        /**
         * Get this node's padding, as defined by style + default padding.
         */
        get() = style.padding

    fun setPadding(spacingType: Int, padding: Float) {
        if (style.padding.set(spacingType, padding)) {
            dirty()
        }
    }

    val border: Spacing
        /**
         * Get this node's border, as defined by style.
         */
        get() = style.border

    fun setBorder(spacingType: Int, border: Float) {
        if (style.border.set(spacingType, border)) {
            dirty()
        }
    }

    var positionTop: Float
        /**
         * Get this node's position top, as defined by style.
         */
        get() = style.position[CSSLayout.Companion.POSITION_TOP]
        set(positionTop) {
            if (!valuesEqual(style.position[CSSLayout.Companion.POSITION_TOP], positionTop)) {
                style.position[CSSLayout.Companion.POSITION_TOP] = positionTop
                dirty()
            }
        }

    var positionBottom: Float
        /**
         * Get this node's position bottom, as defined by style.
         */
        get() = style.position[CSSLayout.Companion.POSITION_BOTTOM]
        set(positionBottom) {
            if (!valuesEqual(style.position[CSSLayout.Companion.POSITION_BOTTOM], positionBottom)) {
                style.position[CSSLayout.Companion.POSITION_BOTTOM] = positionBottom
                dirty()
            }
        }

    var positionLeft: Float
        /**
         * Get this node's position left, as defined by style.
         */
        get() = style.position[CSSLayout.Companion.POSITION_LEFT]
        set(positionLeft) {
            if (!valuesEqual(style.position[CSSLayout.Companion.POSITION_LEFT], positionLeft)) {
                style.position[CSSLayout.Companion.POSITION_LEFT] = positionLeft
                dirty()
            }
        }

    var positionRight: Float
        /**
         * Get this node's position right, as defined by style.
         */
        get() = style.position[CSSLayout.Companion.POSITION_RIGHT]
        set(positionRight) {
            if (!valuesEqual(style.position[CSSLayout.Companion.POSITION_RIGHT], positionRight)) {
                style.position[CSSLayout.Companion.POSITION_RIGHT] = positionRight
                dirty()
            }
        }

    var styleWidth: Float
        /**
         * Get this node's width, as defined in the style.
         */
        get() = style.dimensions[CSSLayout.Companion.DIMENSION_WIDTH]
        set(width) {
            if (!valuesEqual(style.dimensions[CSSLayout.Companion.DIMENSION_WIDTH], width)) {
                style.dimensions[CSSLayout.Companion.DIMENSION_WIDTH] = width
                dirty()
            }
        }

    var styleHeight: Float
        /**
         * Get this node's height, as defined in the style.
         */
        get() = style.dimensions[CSSLayout.Companion.DIMENSION_HEIGHT]
        set(height) {
            if (!valuesEqual(style.dimensions[CSSLayout.Companion.DIMENSION_HEIGHT], height)) {
                style.dimensions[CSSLayout.Companion.DIMENSION_HEIGHT] = height
                dirty()
            }
        }

    val layoutX: Float
        get() = layout.position[CSSLayout.Companion.POSITION_LEFT]

    val layoutY: Float
        get() = layout.position[CSSLayout.Companion.POSITION_TOP]

    val layoutWidth: Float
        get() = layout.dimensions[CSSLayout.Companion.DIMENSION_WIDTH]

    val layoutHeight: Float
        get() = layout.dimensions[CSSLayout.Companion.DIMENSION_HEIGHT]

    val layoutDirection: CSSDirection?
        get() = layout.direction

    /**
     * Set a default padding (left/top/right/bottom) for this node.
     */
    fun setDefaultPadding(spacingType: Int, padding: Float) {
        if (style.padding.setDefault(spacingType, padding)) {
            dirty()
        }
    }

    /**
     * Resets this instance to its default state. This method is meant to be used when
     * recycling [CSSNode] instances.
     */
    fun reset() {
        check(!(this.parent != null || (mChildren != null && mChildren!!.size > 0))) { "You should not reset an attached CSSNode" }

        style.reset()
        layout.resetResult()
        lineIndex = 0
        mLayoutState = LayoutState.DIRTY
    }
}
