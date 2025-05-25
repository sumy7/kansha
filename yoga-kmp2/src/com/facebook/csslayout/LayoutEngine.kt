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
 * Calculates layouts based on CSS style. See [.layoutNode].
 */
object LayoutEngine {
    private val Float = FloatUtil

    private val CSS_FLEX_DIRECTION_COLUMN: Int = CSSFlexDirection.COLUMN.ordinal
    private val CSS_FLEX_DIRECTION_COLUMN_REVERSE: Int = CSSFlexDirection.COLUMN_REVERSE.ordinal
    private val CSS_FLEX_DIRECTION_ROW: Int = CSSFlexDirection.ROW.ordinal
    private val CSS_FLEX_DIRECTION_ROW_REVERSE: Int = CSSFlexDirection.ROW_REVERSE.ordinal

    private val CSS_POSITION_RELATIVE: Int = CSSPositionType.RELATIVE.ordinal
    private val CSS_POSITION_ABSOLUTE: Int = CSSPositionType.ABSOLUTE.ordinal

    private val leading = intArrayOf(
        CSSLayout.Companion.POSITION_TOP,
        CSSLayout.Companion.POSITION_BOTTOM,
        CSSLayout.Companion.POSITION_LEFT,
        CSSLayout.Companion.POSITION_RIGHT,
    )

    private val trailing = intArrayOf(
        CSSLayout.Companion.POSITION_BOTTOM,
        CSSLayout.Companion.POSITION_TOP,
        CSSLayout.Companion.POSITION_RIGHT,
        CSSLayout.Companion.POSITION_LEFT,
    )

    private val pos = intArrayOf(
        CSSLayout.Companion.POSITION_TOP,
        CSSLayout.Companion.POSITION_BOTTOM,
        CSSLayout.Companion.POSITION_LEFT,
        CSSLayout.Companion.POSITION_RIGHT,
    )

    private val dim = intArrayOf(
        CSSLayout.Companion.DIMENSION_HEIGHT,
        CSSLayout.Companion.DIMENSION_HEIGHT,
        CSSLayout.Companion.DIMENSION_WIDTH,
        CSSLayout.Companion.DIMENSION_WIDTH,
    )

    private val leadingSpacing = intArrayOf(
        Spacing.TOP,
        Spacing.BOTTOM,
        Spacing.START,
        Spacing.START
    )

    private val trailingSpacing = intArrayOf(
        Spacing.BOTTOM,
        Spacing.TOP,
        Spacing.END,
        Spacing.END
    )

    private fun boundAxis(node: CSSNode, axis: Int, value: Float): Float {
        var min = CSSConstants.UNDEFINED
        var max = CSSConstants.UNDEFINED

        if (axis == CSS_FLEX_DIRECTION_COLUMN ||
            axis == CSS_FLEX_DIRECTION_COLUMN_REVERSE
        ) {
            min = node.style.minHeight
            max = node.style.maxHeight
        } else if (axis == CSS_FLEX_DIRECTION_ROW ||
            axis == CSS_FLEX_DIRECTION_ROW_REVERSE
        ) {
            min = node.style.minWidth
            max = node.style.maxWidth
        }

        var boundValue = value

        if (!Float.isNaN(max) && max >= 0.0 && boundValue > max) {
            boundValue = max
        }
        if (!Float.isNaN(min) && min >= 0.0 && boundValue < min) {
            boundValue = min
        }

        return boundValue
    }

    private fun setDimensionFromStyle(node: CSSNode, axis: Int) {
        // The parent already computed us a width or height. We just skip it
        if (!Float.isNaN(node.layout.dimensions[dim[axis]])) {
            return
        }
        // We only run if there's a width or height defined
        if (Float.isNaN(node.style.dimensions[dim[axis]]) ||
            node.style.dimensions[dim[axis]] <= 0.0
        ) {
            return
        }

        // The dimensions can never be smaller than the padding and border
        val maxLayoutDimension: Float = maxOf(
            boundAxis(node, axis, node.style.dimensions[dim[axis]]),
            node.style.padding.getWithFallback(leadingSpacing[axis], leading[axis]) +
                    node.style.padding.getWithFallback(trailingSpacing[axis], trailing[axis]) +
                    node.style.border.getWithFallback(leadingSpacing[axis], leading[axis]) +
                    node.style.border.getWithFallback(trailingSpacing[axis], trailing[axis])
        )
        node.layout.dimensions[dim[axis]] = maxLayoutDimension
    }

    private fun getRelativePosition(node: CSSNode, axis: Int): Float {
        val lead: Float = node.style.position[leading[axis]]!!
        if (!Float.isNaN(lead)) {
            return lead
        }

        val trailingPos: Float = node.style.position[trailing[axis]]!!
        return if (Float.isNaN(trailingPos)) 0f else -trailingPos
    }

    private fun resolveAxis(
        axis: Int,
        direction: CSSDirection?
    ): Int {
        if (direction == CSSDirection.RTL) {
            if (axis == CSS_FLEX_DIRECTION_ROW) {
                return CSS_FLEX_DIRECTION_ROW_REVERSE
            } else if (axis == CSS_FLEX_DIRECTION_ROW_REVERSE) {
                return CSS_FLEX_DIRECTION_ROW
            }
        }

        return axis
    }

    private fun resolveDirection(node: CSSNode, parentDirection: CSSDirection?): CSSDirection? {
        var direction: CSSDirection? = node.style.direction
        if (direction == CSSDirection.INHERIT) {
            direction = (if (parentDirection == null) CSSDirection.LTR else parentDirection)
        }

        return direction
    }

    private fun getFlexDirection(node: CSSNode): Int {
        return node.style.flexDirection!!.ordinal
    }

    private fun getCrossFlexDirection(
        axis: Int,
        direction: CSSDirection?
    ): Int {
        if (axis == CSS_FLEX_DIRECTION_COLUMN ||
            axis == CSS_FLEX_DIRECTION_COLUMN_REVERSE
        ) {
            return resolveAxis(CSS_FLEX_DIRECTION_ROW, direction)
        } else {
            return CSS_FLEX_DIRECTION_COLUMN
        }
    }

    private fun getAlignItem(node: CSSNode, child: CSSNode): CSSAlign {
        if (child.style.alignSelf !== CSSAlign.AUTO) {
            return child.style.alignSelf!!
        }
        return node.style.alignItems!!
    }

    private fun isMeasureDefined(node: CSSNode): Boolean {
        return node.isMeasureDefined
    }

    fun needsRelayout(node: CSSNode, parentMaxWidth: Float): Boolean {
        return node.isDirty || !FloatUtil.floatsEqual(
            node.lastLayout.requestedHeight,
            node.layout.dimensions[CSSLayout.Companion.DIMENSION_HEIGHT]
        ) || !FloatUtil.floatsEqual(
            node.lastLayout.requestedWidth,
            node.layout.dimensions[CSSLayout.Companion.DIMENSION_WIDTH]
        ) || !FloatUtil.floatsEqual(node.lastLayout.parentMaxWidth, parentMaxWidth)
    }

    /*package*/
    fun layoutNode(
        layoutContext: CSSLayoutContext,
        node: CSSNode,
        parentMaxWidth: Float,
        parentDirection: CSSDirection?
    ) {
        if (needsRelayout(node, parentMaxWidth)) {
            node.lastLayout.requestedWidth = node.layout.dimensions[CSSLayout.Companion.DIMENSION_WIDTH]
            node.lastLayout.requestedHeight = node.layout.dimensions[CSSLayout.Companion.DIMENSION_HEIGHT]
            node.lastLayout.parentMaxWidth = parentMaxWidth

            layoutNodeImpl(layoutContext, node, parentMaxWidth, parentDirection)
            node.lastLayout.copy(node.layout)
        } else {
            node.layout.copy(node.lastLayout)
        }

        node.markHasNewLayout()
    }

    private fun layoutNodeImpl(
        layoutContext: CSSLayoutContext,
        node: CSSNode,
        parentMaxWidth: Float,
        parentDirection: CSSDirection?
    ) {
        run {
            var i = 0
            val childCount: Int = node.childCount
            while (i < childCount) {
                node.getChildAt(i).layout.resetResult()
                i++
            }
        }

        /** START_GENERATED  */
        val direction = resolveDirection(node, parentDirection)
        val mainAxis = resolveAxis(getFlexDirection(node), direction)
        val crossAxis = getCrossFlexDirection(mainAxis, direction)
        val resolvedRowAxis = resolveAxis(CSS_FLEX_DIRECTION_ROW, direction)


        // Handle width and height style attributes
        setDimensionFromStyle(node, mainAxis)
        setDimensionFromStyle(node, crossAxis)


        // Set the resolved resolution in the node's layout
        node.layout.direction = direction


        // The position is set by the parent, but we need to complete it with a
        // delta composed of the margin and left/top/right/bottom
        node.layout.position[leading[mainAxis]] += node.style.margin.getWithFallback(
            leadingSpacing[mainAxis],
            leading[mainAxis]
        ) +
                getRelativePosition(node, mainAxis)
        node.layout.position[trailing[mainAxis]] += node.style.margin.getWithFallback(
            trailingSpacing[mainAxis],
            trailing[mainAxis]
        ) +
                getRelativePosition(node, mainAxis)
        node.layout.position[leading[crossAxis]] += node.style.margin.getWithFallback(
            leadingSpacing[crossAxis],
            leading[crossAxis]
        ) +
                getRelativePosition(node, crossAxis)
        node.layout.position[trailing[crossAxis]] += node.style.margin.getWithFallback(
            trailingSpacing[crossAxis],
            trailing[crossAxis]
        ) +
                getRelativePosition(node, crossAxis)


        // Inline immutable values from the target node to avoid excessive method
        // invocations during the layout calculation.
        val childCount: Int = node.childCount
        val paddingAndBorderAxisResolvedRow: Float = ((node.style.padding.getWithFallback(
            leadingSpacing[resolvedRowAxis],
            leading[resolvedRowAxis]
        ) + node.style.border.getWithFallback(
            leadingSpacing[resolvedRowAxis], leading[resolvedRowAxis]
        )) + (node.style.padding.getWithFallback(
            trailingSpacing[resolvedRowAxis], trailing[resolvedRowAxis]
        ) + node.style.border.getWithFallback(
            trailingSpacing[resolvedRowAxis], trailing[resolvedRowAxis]
        )))

        if (isMeasureDefined(node)) {
            val isResolvedRowDimDefined = !Float.isNaN(node.layout.dimensions[dim[resolvedRowAxis]])

            var width = CSSConstants.UNDEFINED
            if ((!Float.isNaN(node.style.dimensions[dim[resolvedRowAxis]]) && node.style.dimensions[dim[resolvedRowAxis]] >= 0.0)) {
                width = node.style.dimensions[CSSLayout.Companion.DIMENSION_WIDTH]
            } else if (isResolvedRowDimDefined) {
                width = node.layout.dimensions[dim[resolvedRowAxis]]
            } else {
                width = parentMaxWidth -
                        (node.style.margin.getWithFallback(
                            leadingSpacing[resolvedRowAxis],
                            leading[resolvedRowAxis]
                        ) + node.style.margin.getWithFallback(
                            trailingSpacing[resolvedRowAxis], trailing[resolvedRowAxis]
                        ))
            }
            width -= paddingAndBorderAxisResolvedRow


            // We only need to give a dimension for the text if we haven't got any
            // for it computed yet. It can either be from the style attribute or because
            // the element is flexible.
            val isRowUndefined =
                !(!Float.isNaN(node.style.dimensions[dim[resolvedRowAxis]]) && node.style.dimensions[dim[resolvedRowAxis]] >= 0.0) && !isResolvedRowDimDefined
            val isColumnUndefined =
                !(!Float.isNaN(node.style.dimensions[dim[CSS_FLEX_DIRECTION_COLUMN]]) && node.style.dimensions[dim[CSS_FLEX_DIRECTION_COLUMN]] >= 0.0) &&
                        Float.isNaN(node.layout.dimensions[dim[CSS_FLEX_DIRECTION_COLUMN]])


            // Let's not measure the text if we already know both dimensions
            if (isRowUndefined || isColumnUndefined) {
                val measureDim: MeasureOutput = node.measure(
                    layoutContext.measureOutput,
                    width
                )
                if (isRowUndefined) {
                    node.layout.dimensions[CSSLayout.Companion.DIMENSION_WIDTH] = measureDim.width +
                            paddingAndBorderAxisResolvedRow
                }
                if (isColumnUndefined) {
                    node.layout.dimensions[CSSLayout.Companion.DIMENSION_HEIGHT] = measureDim.height +
                            ((node.style.padding.getWithFallback(
                                leadingSpacing[CSS_FLEX_DIRECTION_COLUMN],
                                leading[CSS_FLEX_DIRECTION_COLUMN]
                            ) + node.style.border.getWithFallback(
                                leadingSpacing[CSS_FLEX_DIRECTION_COLUMN], leading[CSS_FLEX_DIRECTION_COLUMN]
                            )) + (node.style.padding.getWithFallback(
                                trailingSpacing[CSS_FLEX_DIRECTION_COLUMN], trailing[CSS_FLEX_DIRECTION_COLUMN]
                            ) + node.style.border.getWithFallback(
                                trailingSpacing[CSS_FLEX_DIRECTION_COLUMN], trailing[CSS_FLEX_DIRECTION_COLUMN]
                            )))
                }
            }
            if (childCount == 0) {
                return
            }
        }

        val isNodeFlexWrap = (node.style.flexWrap === CSSWrap.WRAP)

        val justifyContent: CSSJustify? = node.style.justifyContent

        val leadingPaddingAndBorderMain: Float = (node.style.padding.getWithFallback(
            leadingSpacing[mainAxis],
            leading[mainAxis]
        ) + node.style.border.getWithFallback(
            leadingSpacing[mainAxis], leading[mainAxis]
        ))
        val leadingPaddingAndBorderCross: Float = (node.style.padding.getWithFallback(
            leadingSpacing[crossAxis],
            leading[crossAxis]
        ) + node.style.border.getWithFallback(
            leadingSpacing[crossAxis], leading[crossAxis]
        ))
        val paddingAndBorderAxisMain: Float = ((node.style.padding.getWithFallback(
            leadingSpacing[mainAxis],
            leading[mainAxis]
        ) + node.style.border.getWithFallback(
            leadingSpacing[mainAxis], leading[mainAxis]
        )) + (node.style.padding.getWithFallback(
            trailingSpacing[mainAxis],
            trailing[mainAxis]
        ) + node.style.border.getWithFallback(
            trailingSpacing[mainAxis], trailing[mainAxis]
        )))
        val paddingAndBorderAxisCross: Float = ((node.style.padding.getWithFallback(
            leadingSpacing[crossAxis],
            leading[crossAxis]
        ) + node.style.border.getWithFallback(
            leadingSpacing[crossAxis], leading[crossAxis]
        )) + (node.style.padding.getWithFallback(
            trailingSpacing[crossAxis],
            trailing[crossAxis]
        ) + node.style.border.getWithFallback(
            trailingSpacing[crossAxis], trailing[crossAxis]
        )))

        val isMainDimDefined = !Float.isNaN(node.layout.dimensions[dim[mainAxis]])
        val isCrossDimDefined = !Float.isNaN(node.layout.dimensions[dim[crossAxis]])
        val isMainRowDirection = (mainAxis == CSS_FLEX_DIRECTION_ROW || mainAxis == CSS_FLEX_DIRECTION_ROW_REVERSE)

        var i: Int
        var ii: Int
        var child: CSSNode?
        var axis: Int

        var firstAbsoluteChild: CSSNode? = null
        var currentAbsoluteChild: CSSNode? = null

        var definedMainDim = CSSConstants.UNDEFINED
        if (isMainDimDefined) {
            definedMainDim = node.layout.dimensions[dim[mainAxis]] - paddingAndBorderAxisMain
        }


        // We want to execute the next two loops one per line with flex-wrap
        var startLine = 0
        var endLine = 0
        // int nextOffset = 0;
        var alreadyComputedNextLayout = 0
        // We aggregate the total dimensions of the container in those two variables
        var linesCrossDim = 0f
        var linesMainDim = 0f
        var linesCount = 0
        while (endLine < childCount) {
            // <Loop A> Layout non flexible children and count children by type

            // mainContentDim is accumulation of the dimensions and margin of all the
            // non flexible children. This will be used in order to either set the
            // dimensions of the node if none already exist, or to compute the
            // remaining space left for the flexible children.

            var mainContentDim = 0f


            // There are three kind of children, non flexible, flexible and absolute.
            // We need to know how many there are in order to distribute the space.
            var flexibleChildrenCount = 0
            var totalFlexible = 0f
            var nonFlexibleChildrenCount = 0


            // Use the line loop to position children in the main axis for as long
            // as they are using a simple stacking behaviour. Children that are
            // immediately stacked in the initial loop will not be touched again
            // in <Loop C>.
            var isSimpleStackMain =
                (isMainDimDefined && justifyContent == CSSJustify.FLEX_START) ||
                        (!isMainDimDefined && justifyContent != CSSJustify.CENTER)
            var firstComplexMain = (if (isSimpleStackMain) childCount else startLine)


            // Use the initial line loop to position children in the cross axis for
            // as long as they are relatively positioned with alignment STRETCH or
            // FLEX_START. Children that are immediately stacked in the initial loop
            // will not be touched again in <Loop D>.
            var isSimpleStackCross = true
            var firstComplexCross = childCount

            var firstFlexChild: CSSNode? = null
            var currentFlexChild: CSSNode? = null

            var mainDim = leadingPaddingAndBorderMain
            var crossDim = 0f

            var maxWidth: Float
            i = startLine
            while (i < childCount) {
                child = node.getChildAt(i)
                child.lineIndex = linesCount

                child.nextAbsoluteChild = null
                child.nextFlexChild = null

                val alignItem = getAlignItem(node, child)


                // Pre-fill cross axis dimensions when the child is using stretch before
                // we call the recursive layout pass
                if (alignItem == CSSAlign.STRETCH && child.style.positionType === CSSPositionType.RELATIVE &&
                    isCrossDimDefined && !(!Float.isNaN(child.style.dimensions[dim[crossAxis]]) && child.style.dimensions[dim[crossAxis]] >= 0.0)
                ) {
                    child.layout.dimensions[dim[crossAxis]] = maxOf(
                        boundAxis(
                            child, crossAxis, node.layout.dimensions[dim[crossAxis]] -
                                    paddingAndBorderAxisCross - (child.style.margin.getWithFallback(
                                leadingSpacing[crossAxis],
                                leading[crossAxis]
                            ) + child.style.margin.getWithFallback(
                                trailingSpacing[crossAxis], trailing[crossAxis]
                            ))
                        ),  // You never want to go smaller than padding
                        ((child.style.padding.getWithFallback(
                            leadingSpacing[crossAxis],
                            leading[crossAxis]
                        ) + child.style.border.getWithFallback(
                            leadingSpacing[crossAxis], leading[crossAxis]
                        )) + (child.style.padding.getWithFallback(
                            trailingSpacing[crossAxis], trailing[crossAxis]
                        ) + child.style.border.getWithFallback(
                            trailingSpacing[crossAxis], trailing[crossAxis]
                        )))
                    )
                } else if (child.style.positionType === CSSPositionType.ABSOLUTE) {
                    // Store a private linked list of absolutely positioned children
                    // so that we can efficiently traverse them later.
                    if (firstAbsoluteChild == null) {
                        firstAbsoluteChild = child
                    }
                    if (currentAbsoluteChild != null) {
                        currentAbsoluteChild.nextAbsoluteChild = child
                    }
                    currentAbsoluteChild = child


                    // Pre-fill dimensions when using absolute position and both offsets for the axis are defined (either both
                    // left and right or top and bottom).
                    ii = 0
                    while (ii < 2) {
                        axis = if (ii != 0) CSS_FLEX_DIRECTION_ROW else CSS_FLEX_DIRECTION_COLUMN
                        if (!Float.isNaN(node.layout.dimensions[dim[axis]]) && !(!Float.isNaN(child.style.dimensions[dim[axis]]) && child.style.dimensions[dim[axis]] >= 0.0) && !Float.isNaN(
                                child.style.position[leading[axis]]
                            ) && !Float.isNaN(child.style.position[trailing[axis]])
                        ) {
                            child.layout.dimensions[dim[axis]] = maxOf(
                                boundAxis(
                                    child, axis, node.layout.dimensions[dim[axis]] -
                                            ((node.style.padding.getWithFallback(
                                                leadingSpacing[axis],
                                                leading[axis]
                                            ) + node.style.border.getWithFallback(
                                                leadingSpacing[axis], leading[axis]
                                            )) + (node.style.padding.getWithFallback(
                                                trailingSpacing[axis], trailing[axis]
                                            ) + node.style.border.getWithFallback(
                                                trailingSpacing[axis], trailing[axis]
                                            ))) -
                                            (child.style.margin.getWithFallback(
                                                leadingSpacing[axis],
                                                leading[axis]
                                            ) + child.style.margin.getWithFallback(
                                                trailingSpacing[axis], trailing[axis]
                                            )) -
                                            (if (Float.isNaN(child.style.position[leading[axis]])) 0f else child.style.position[leading[axis]]) -
                                            (if (Float.isNaN(child.style.position[trailing[axis]])) 0f else child.style.position[trailing[axis]])
                                ),  // You never want to go smaller than padding
                                ((child.style.padding.getWithFallback(
                                    leadingSpacing[axis],
                                    leading[axis]
                                ) + child.style.border.getWithFallback(
                                    leadingSpacing[axis], leading[axis]
                                )) + (child.style.padding.getWithFallback(
                                    trailingSpacing[axis], trailing[axis]
                                ) + child.style.border.getWithFallback(
                                    trailingSpacing[axis], trailing[axis]
                                )))
                            )
                        }
                        ii++
                    }
                }

                var nextContentDim = 0f


                // It only makes sense to consider a child flexible if we have a computed
                // dimension for the node.
                if (isMainDimDefined && (child.style.positionType === CSSPositionType.RELATIVE && child.style.flex > 0)) {
                    flexibleChildrenCount++
                    totalFlexible += child.style.flex


                    // Store a private linked list of flexible children so that we can
                    // efficiently traverse them later.
                    if (firstFlexChild == null) {
                        firstFlexChild = child
                    }
                    if (currentFlexChild != null) {
                        currentFlexChild.nextFlexChild = child
                    }
                    currentFlexChild = child


                    // Even if we don't know its exact size yet, we already know the padding,
                    // border and margin. We'll use this partial information, which represents
                    // the smallest possible size for the child, to compute the remaining
                    // available space.
                    nextContentDim = ((child.style.padding.getWithFallback(
                        leadingSpacing[mainAxis],
                        leading[mainAxis]
                    ) + child.style.border.getWithFallback(
                        leadingSpacing[mainAxis], leading[mainAxis]
                    )) + (child.style.padding.getWithFallback(
                        trailingSpacing[mainAxis], trailing[mainAxis]
                    ) + child.style.border.getWithFallback(
                        trailingSpacing[mainAxis], trailing[mainAxis]
                    ))) +
                            (child.style.margin.getWithFallback(
                                leadingSpacing[mainAxis],
                                leading[mainAxis]
                            ) + child.style.margin.getWithFallback(
                                trailingSpacing[mainAxis], trailing[mainAxis]
                            ))
                } else {
                    maxWidth = CSSConstants.UNDEFINED
                    if (!isMainRowDirection) {
                        if ((!Float.isNaN(node.style.dimensions[dim[resolvedRowAxis]]) && node.style.dimensions[dim[resolvedRowAxis]] >= 0.0)) {
                            maxWidth = node.layout.dimensions[dim[resolvedRowAxis]] -
                                    paddingAndBorderAxisResolvedRow
                        } else {
                            maxWidth = parentMaxWidth -
                                    (node.style.margin.getWithFallback(
                                        leadingSpacing[resolvedRowAxis],
                                        leading[resolvedRowAxis]
                                    ) + node.style.margin.getWithFallback(
                                        trailingSpacing[resolvedRowAxis], trailing[resolvedRowAxis]
                                    )) -
                                    paddingAndBorderAxisResolvedRow
                        }
                    }


                    // This is the main recursive call. We layout non flexible children.
                    if (alreadyComputedNextLayout == 0) {
                        layoutNode(layoutContext, child, maxWidth, direction)
                    }


                    // Absolute positioned elements do not take part of the layout, so we
                    // don't use them to compute mainContentDim
                    if (child.style.positionType === CSSPositionType.RELATIVE) {
                        nonFlexibleChildrenCount++
                        // At this point we know the final size and margin of the element.
                        nextContentDim = (child.layout.dimensions[dim[mainAxis]] + child.style.margin.getWithFallback(
                            leadingSpacing[mainAxis], leading[mainAxis]
                        ) + child.style.margin.getWithFallback(
                            trailingSpacing[mainAxis], trailing[mainAxis]
                        ))
                    }
                }


                // The element we are about to add would make us go to the next line
                if (isNodeFlexWrap &&
                    isMainDimDefined && mainContentDim + nextContentDim > definedMainDim &&  // If there's only one element, then it's bigger than the content
                    // and needs its own line
                    i != startLine
                ) {
                    nonFlexibleChildrenCount--
                    alreadyComputedNextLayout = 1
                    break
                }


                // Disable simple stacking in the main axis for the current line as
                // we found a non-trivial child. The remaining children will be laid out
                // in <Loop C>.
                if (isSimpleStackMain &&
                    (child.style.positionType !== CSSPositionType.RELATIVE || (child.style.positionType === CSSPositionType.RELATIVE && child.style.flex > 0))
                ) {
                    isSimpleStackMain = false
                    firstComplexMain = i
                }


                // Disable simple stacking in the cross axis for the current line as
                // we found a non-trivial child. The remaining children will be laid out
                // in <Loop D>.
                if (isSimpleStackCross &&
                    (child.style.positionType !== CSSPositionType.RELATIVE ||
                            (alignItem != CSSAlign.STRETCH && alignItem != CSSAlign.FLEX_START) ||
                            Float.isNaN(child.layout.dimensions[dim[crossAxis]]))
                ) {
                    isSimpleStackCross = false
                    firstComplexCross = i
                }

                if (isSimpleStackMain) {
                    child.layout.position[pos[mainAxis]] += mainDim
                    if (isMainDimDefined) {
                        child.layout.position[trailing[mainAxis]] =
                            node.layout.dimensions[dim[mainAxis]] - child.layout.dimensions[dim[mainAxis]] - child.layout.position[pos[mainAxis]]
                    }

                    mainDim += (child.layout.dimensions[dim[mainAxis]] + child.style.margin.getWithFallback(
                        leadingSpacing[mainAxis], leading[mainAxis]
                    ) + child.style.margin.getWithFallback(
                        trailingSpacing[mainAxis], trailing[mainAxis]
                    ))
                    crossDim = maxOf(
                        crossDim, boundAxis(
                            child,
                            crossAxis,
                            (child.layout.dimensions[dim[crossAxis]] + child.style.margin.getWithFallback(
                                leadingSpacing[crossAxis], leading[crossAxis]
                            ) + child.style.margin.getWithFallback(
                                trailingSpacing[crossAxis], trailing[crossAxis]
                            ))
                        )
                    )
                }

                if (isSimpleStackCross) {
                    child.layout.position[pos[crossAxis]] += linesCrossDim + leadingPaddingAndBorderCross
                    if (isCrossDimDefined) {
                        child.layout.position[trailing[crossAxis]] =
                            node.layout.dimensions[dim[crossAxis]] - child.layout.dimensions[dim[crossAxis]] - child.layout.position[pos[crossAxis]]
                    }
                }

                alreadyComputedNextLayout = 0
                mainContentDim += nextContentDim
                endLine = i + 1
                ++i
            }


            // <Loop B> Layout flexible children and allocate empty space

            // In order to position the elements in the main axis, we have two
            // controls. The space between the beginning and the first element
            // and the space between each two elements.
            var leadingMainDim = 0f
            var betweenMainDim = 0f


            // The remaining available space that needs to be allocated
            var remainingMainDim = 0f
            if (isMainDimDefined) {
                remainingMainDim = definedMainDim - mainContentDim
            } else {
                remainingMainDim = maxOf(mainContentDim, 0f) - mainContentDim
            }


            // If there are flexible children in the mix, they are going to fill the
            // remaining space
            if (flexibleChildrenCount != 0) {
                var flexibleMainDim = remainingMainDim / totalFlexible
                var baseMainDim: Float
                var boundMainDim: Float


                // If the flex share of remaining space doesn't meet min/max bounds,
                // remove this child from flex calculations.
                currentFlexChild = firstFlexChild
                while (currentFlexChild != null) {
                    baseMainDim = flexibleMainDim * currentFlexChild.style.flex +
                            ((currentFlexChild.style.padding.getWithFallback(
                                leadingSpacing[mainAxis],
                                leading[mainAxis]
                            ) + currentFlexChild.style.border.getWithFallback(
                                leadingSpacing[mainAxis], leading[mainAxis]
                            )) + (currentFlexChild.style.padding.getWithFallback(
                                trailingSpacing[mainAxis], trailing[mainAxis]
                            ) + currentFlexChild.style.border.getWithFallback(
                                trailingSpacing[mainAxis], trailing[mainAxis]
                            )))
                    boundMainDim = boundAxis(currentFlexChild, mainAxis, baseMainDim)

                    if (baseMainDim != boundMainDim) {
                        remainingMainDim -= boundMainDim
                        totalFlexible -= currentFlexChild.style.flex
                    }

                    currentFlexChild = currentFlexChild.nextFlexChild
                }
                flexibleMainDim = remainingMainDim / totalFlexible


                // The non flexible children can overflow the container, in this case
                // we should just assume that there is no space available.
                if (flexibleMainDim < 0) {
                    flexibleMainDim = 0f
                }

                currentFlexChild = firstFlexChild
                while (currentFlexChild != null) {
                    // At this point we know the final size of the element in the main
                    // dimension
                    currentFlexChild.layout.dimensions[dim[mainAxis]] = boundAxis(
                        currentFlexChild, mainAxis,
                        flexibleMainDim * currentFlexChild.style.flex +
                                ((currentFlexChild.style.padding.getWithFallback(
                                    leadingSpacing[mainAxis],
                                    leading[mainAxis]
                                ) + currentFlexChild.style.border.getWithFallback(
                                    leadingSpacing[mainAxis], leading[mainAxis]
                                )) + (currentFlexChild.style.padding.getWithFallback(
                                    trailingSpacing[mainAxis], trailing[mainAxis]
                                ) + currentFlexChild.style.border.getWithFallback(
                                    trailingSpacing[mainAxis], trailing[mainAxis]
                                )))
                    )

                    maxWidth = CSSConstants.UNDEFINED
                    if ((!Float.isNaN(node.style.dimensions[dim[resolvedRowAxis]]) && node.style.dimensions[dim[resolvedRowAxis]] >= 0.0)) {
                        maxWidth = node.layout.dimensions[dim[resolvedRowAxis]] -
                                paddingAndBorderAxisResolvedRow
                    } else if (!isMainRowDirection) {
                        maxWidth = parentMaxWidth -
                                (node.style.margin.getWithFallback(
                                    leadingSpacing[resolvedRowAxis],
                                    leading[resolvedRowAxis]
                                ) + node.style.margin.getWithFallback(
                                    trailingSpacing[resolvedRowAxis], trailing[resolvedRowAxis]
                                )) -
                                paddingAndBorderAxisResolvedRow
                    }


                    // And we recursively call the layout algorithm for this child
                    layoutNode(layoutContext, currentFlexChild, maxWidth, direction)

                    child = currentFlexChild
                    currentFlexChild = currentFlexChild.nextFlexChild
                    child.nextFlexChild = null
                }


                // We use justifyContent to figure out how to allocate the remaining
                // space available
            } else if (justifyContent != CSSJustify.FLEX_START) {
                if (justifyContent == CSSJustify.CENTER) {
                    leadingMainDim = remainingMainDim / 2
                } else if (justifyContent == CSSJustify.FLEX_END) {
                    leadingMainDim = remainingMainDim
                } else if (justifyContent == CSSJustify.SPACE_BETWEEN) {
                    remainingMainDim = maxOf(remainingMainDim, 0f)
                    if (flexibleChildrenCount + nonFlexibleChildrenCount - 1 != 0) {
                        betweenMainDim = remainingMainDim /
                                (flexibleChildrenCount + nonFlexibleChildrenCount - 1)
                    } else {
                        betweenMainDim = 0f
                    }
                } else if (justifyContent == CSSJustify.SPACE_AROUND) {
                    // Space on the edges is half of the space between elements
                    betweenMainDim = remainingMainDim /
                            (flexibleChildrenCount + nonFlexibleChildrenCount)
                    leadingMainDim = betweenMainDim / 2
                }
            }


            // <Loop C> Position elements in the main axis and compute dimensions

            // At this point, all the children have their dimensions set. We need to
            // find their position. In order to do that, we accumulate data in
            // variables that are also useful to compute the total dimensions of the
            // container!
            mainDim += leadingMainDim

            i = firstComplexMain
            while (i < endLine) {
                child = node.getChildAt(i)

                if (child.style.positionType === CSSPositionType.ABSOLUTE &&
                    !Float.isNaN(child.style.position[leading[mainAxis]])
                ) {
                    // In case the child is position absolute and has left/top being
                    // defined, we override the position to whatever the user said
                    // (and margin/border).
                    child.layout.position[pos[mainAxis]] =
                        (if (Float.isNaN(child.style.position[leading[mainAxis]])) 0f else child.style.position[leading[mainAxis]]) +
                                node.style.border.getWithFallback(leadingSpacing[mainAxis], leading[mainAxis]) +
                                child.style.margin.getWithFallback(leadingSpacing[mainAxis], leading[mainAxis])
                } else {
                    // If the child is position absolute (without top/left) or relative,
                    // we put it at the current accumulated offset.
                    child.layout.position[pos[mainAxis]] += mainDim


                    // Define the trailing position accordingly.
                    if (isMainDimDefined) {
                        child.layout.position[trailing[mainAxis]] =
                            node.layout.dimensions[dim[mainAxis]] - child.layout.dimensions[dim[mainAxis]] - child.layout.position[pos[mainAxis]]
                    }


                    // Now that we placed the element, we need to update the variables
                    // We only need to do that for relative elements. Absolute elements
                    // do not take part in that phase.
                    if (child.style.positionType === CSSPositionType.RELATIVE) {
                        // The main dimension is the sum of all the elements dimension plus
                        // the spacing.
                        mainDim += betweenMainDim + (child.layout.dimensions[dim[mainAxis]] + child.style.margin.getWithFallback(
                            leadingSpacing[mainAxis], leading[mainAxis]
                        ) + child.style.margin.getWithFallback(
                            trailingSpacing[mainAxis], trailing[mainAxis]
                        ))
                        // The cross dimension is the max of the elements dimension since there
                        // can only be one element in that cross dimension.
                        crossDim = maxOf(
                            crossDim, boundAxis(
                                child,
                                crossAxis,
                                (child.layout.dimensions[dim[crossAxis]] + child.style.margin.getWithFallback(
                                    leadingSpacing[crossAxis], leading[crossAxis]
                                ) + child.style.margin.getWithFallback(
                                    trailingSpacing[crossAxis], trailing[crossAxis]
                                ))
                            )
                        )
                    }
                }
                ++i
            }

            var containerCrossAxis: Float = node.layout.dimensions[dim[crossAxis]]!!
            if (!isCrossDimDefined) {
                containerCrossAxis = maxOf( // For the cross dim, we add both sides at the end because the value
                    // is aggregate via a max function. Intermediate negative values
                    // can mess this computation otherwise
                    boundAxis(node, crossAxis, crossDim + paddingAndBorderAxisCross),
                    paddingAndBorderAxisCross
                )
            }


            // <Loop D> Position elements in the cross axis
            i = firstComplexCross
            while (i < endLine) {
                child = node.getChildAt(i)

                if (child.style.positionType === CSSPositionType.ABSOLUTE &&
                    !Float.isNaN(child.style.position[leading[crossAxis]])
                ) {
                    // In case the child is absolutely positionned and has a
                    // top/left/bottom/right being set, we override all the previously
                    // computed positions to set it correctly.
                    child.layout.position[pos[crossAxis]] =
                        (if (Float.isNaN(child.style.position[leading[crossAxis]])) 0f else child.style.position[leading[crossAxis]]) +
                                node.style.border.getWithFallback(leadingSpacing[crossAxis], leading[crossAxis]) +
                                child.style.margin.getWithFallback(leadingSpacing[crossAxis], leading[crossAxis])
                } else {
                    var leadingCrossDim = leadingPaddingAndBorderCross


                    // For a relative children, we're either using alignItems (parent) or
                    // alignSelf (child) in order to determine the position in the cross axis
                    if (child.style.positionType === CSSPositionType.RELATIVE) {
                        /*eslint-disable */
                        // This variable is intentionally re-defined as the code is transpiled to a block scope language
                        val alignItem = getAlignItem(node, child)
                        /*eslint-enable */
                        if (alignItem == CSSAlign.STRETCH) {
                            // You can only stretch if the dimension has not already been set
                            // previously.
                            if (Float.isNaN(child.layout.dimensions[dim[crossAxis]])) {
                                child.layout.dimensions[dim[crossAxis]] = maxOf(
                                    boundAxis(
                                        child, crossAxis, containerCrossAxis -
                                                paddingAndBorderAxisCross - (child.style.margin.getWithFallback(
                                            leadingSpacing[crossAxis],
                                            leading[crossAxis]
                                        ) + child.style.margin.getWithFallback(
                                            trailingSpacing[crossAxis], trailing[crossAxis]
                                        ))
                                    ),  // You never want to go smaller than padding
                                    ((child.style.padding.getWithFallback(
                                        leadingSpacing[crossAxis],
                                        leading[crossAxis]
                                    ) + child.style.border.getWithFallback(
                                        leadingSpacing[crossAxis], leading[crossAxis]
                                    )) + (child.style.padding.getWithFallback(
                                        trailingSpacing[crossAxis], trailing[crossAxis]
                                    ) + child.style.border.getWithFallback(
                                        trailingSpacing[crossAxis], trailing[crossAxis]
                                    )))
                                )
                            }
                        } else if (alignItem != CSSAlign.FLEX_START) {
                            // The remaining space between the parent dimensions+padding and child
                            // dimensions+margin.
                            val remainingCrossDim: Float = containerCrossAxis -
                                    paddingAndBorderAxisCross - (child.layout.dimensions[dim[crossAxis]] + child.style.margin.getWithFallback(
                                leadingSpacing[crossAxis], leading[crossAxis]
                            ) + child.style.margin.getWithFallback(
                                trailingSpacing[crossAxis], trailing[crossAxis]
                            ))

                            if (alignItem == CSSAlign.CENTER) {
                                leadingCrossDim += remainingCrossDim / 2
                            } else { // CSSAlign.FLEX_END
                                leadingCrossDim += remainingCrossDim
                            }
                        }
                    }


                    // And we apply the position
                    child.layout.position[pos[crossAxis]] += linesCrossDim + leadingCrossDim


                    // Define the trailing position accordingly.
                    if (isCrossDimDefined) {
                        child.layout.position[trailing[crossAxis]] =
                            node.layout.dimensions[dim[crossAxis]] - child.layout.dimensions[dim[crossAxis]] - child.layout.position[pos[crossAxis]]
                    }
                }
                ++i
            }

            linesCrossDim += crossDim
            linesMainDim = maxOf(linesMainDim, mainDim)
            linesCount += 1
            startLine = endLine
        }


        // <Loop E>
        //
        // Note(prenaux): More than one line, we need to layout the crossAxis
        // according to alignContent.
        //
        // Note that we could probably remove <Loop D> and handle the one line case
        // here too, but for the moment this is safer since it won't interfere with
        // previously working code.
        //
        // See specs:
        // http://www.w3.org/TR/2012/CR-css3-flexbox-20120918/#layout-algorithm
        // section 9.4
        //
        if (linesCount > 1 && isCrossDimDefined) {
            val nodeCrossAxisInnerSize: Float = node.layout.dimensions[dim[crossAxis]] -
                    paddingAndBorderAxisCross
            val remainingAlignContentDim = nodeCrossAxisInnerSize - linesCrossDim

            var crossDimLead = 0f
            var currentLead = leadingPaddingAndBorderCross

            val alignContent: CSSAlign? = node.style.alignContent
            if (alignContent == CSSAlign.FLEX_END) {
                currentLead += remainingAlignContentDim
            } else if (alignContent == CSSAlign.CENTER) {
                currentLead += remainingAlignContentDim / 2
            } else if (alignContent == CSSAlign.STRETCH) {
                if (nodeCrossAxisInnerSize > linesCrossDim) {
                    crossDimLead = (remainingAlignContentDim / linesCount)
                }
            }

            var endIndex = 0
            i = 0
            while (i < linesCount) {
                val startIndex = endIndex


                // compute the line's height and find the endIndex
                var lineHeight = 0f
                ii = startIndex
                while (ii < childCount) {
                    child = node.getChildAt(ii)
                    if (child.style.positionType !== CSSPositionType.RELATIVE) {
                        ++ii
                        continue
                    }
                    if (child.lineIndex !== i) {
                        break
                    }
                    if (!Float.isNaN(child.layout.dimensions[dim[crossAxis]])) {
                        lineHeight = maxOf(
                            lineHeight,
                            child.layout.dimensions[dim[crossAxis]] + (child.style.margin.getWithFallback(
                                leadingSpacing[crossAxis],
                                leading[crossAxis]
                            ) + child.style.margin.getWithFallback(
                                trailingSpacing[crossAxis], trailing[crossAxis]
                            ))
                        )
                    }
                    ++ii
                }
                endIndex = ii
                lineHeight += crossDimLead

                ii = startIndex
                while (ii < endIndex) {
                    child = node.getChildAt(ii)
                    if (child.style.positionType !== CSSPositionType.RELATIVE) {
                        ++ii
                        continue
                    }

                    val alignContentAlignItem = getAlignItem(node, child)
                    if (alignContentAlignItem == CSSAlign.FLEX_START) {
                        child.layout.position[pos[crossAxis]] = currentLead + child.style.margin.getWithFallback(
                            leadingSpacing[crossAxis], leading[crossAxis]
                        )
                    } else if (alignContentAlignItem == CSSAlign.FLEX_END) {
                        child.layout.position[pos[crossAxis]] =
                            currentLead + lineHeight - child.style.margin.getWithFallback(
                                trailingSpacing[crossAxis], trailing[crossAxis]
                            ) - child.layout.dimensions[dim[crossAxis]]
                    } else if (alignContentAlignItem == CSSAlign.CENTER) {
                        val childHeight: Float = child.layout.dimensions[dim[crossAxis]]!!
                        child.layout.position[pos[crossAxis]] = currentLead + (lineHeight - childHeight) / 2
                    } else if (alignContentAlignItem == CSSAlign.STRETCH) {
                        child.layout.position[pos[crossAxis]] = currentLead + child.style.margin.getWithFallback(
                            leadingSpacing[crossAxis], leading[crossAxis]
                        )
                        // TODO(prenaux): Correctly set the height of items with undefined
                        //                (auto) crossAxis dimension.
                    }
                    ++ii
                }

                currentLead += lineHeight
                ++i
            }
        }

        var needsMainTrailingPos = false
        var needsCrossTrailingPos = false


        // If the user didn't specify a width or height, and it has not been set
        // by the container, then we set it via the children.
        if (!isMainDimDefined) {
            node.layout.dimensions[dim[mainAxis]] =
                maxOf( // We're missing the last padding at this point to get the final
                    // dimension
                    boundAxis(
                        node,
                        mainAxis,
                        linesMainDim + (node.style.padding.getWithFallback(
                            trailingSpacing[mainAxis],
                            trailing[mainAxis]
                        ) + node.style.border.getWithFallback(
                            trailingSpacing[mainAxis], trailing[mainAxis]
                        ))
                    ),  // We can never assign a width smaller than the padding and borders
                    paddingAndBorderAxisMain
                )

            if (mainAxis == CSS_FLEX_DIRECTION_ROW_REVERSE ||
                mainAxis == CSS_FLEX_DIRECTION_COLUMN_REVERSE
            ) {
                needsMainTrailingPos = true
            }
        }

        if (!isCrossDimDefined) {
            node.layout.dimensions[dim[crossAxis]] =
                maxOf( // For the cross dim, we add both sides at the end because the value
                    // is aggregate via a max function. Intermediate negative values
                    // can mess this computation otherwise
                    boundAxis(node, crossAxis, linesCrossDim + paddingAndBorderAxisCross),
                    paddingAndBorderAxisCross
                )

            if (crossAxis == CSS_FLEX_DIRECTION_ROW_REVERSE ||
                crossAxis == CSS_FLEX_DIRECTION_COLUMN_REVERSE
            ) {
                needsCrossTrailingPos = true
            }
        }


        // <Loop F> Set trailing position if necessary
        if (needsMainTrailingPos || needsCrossTrailingPos) {
            i = 0
            while (i < childCount) {
                child = node.getChildAt(i)

                if (needsMainTrailingPos) {
                    child.layout.position[trailing[mainAxis]] =
                        node.layout.dimensions[dim[mainAxis]] - child.layout.dimensions[dim[mainAxis]] - child.layout.position[pos[mainAxis]]
                }

                if (needsCrossTrailingPos) {
                    child.layout.position[trailing[crossAxis]] =
                        node.layout.dimensions[dim[crossAxis]] - child.layout.dimensions[dim[crossAxis]] - child.layout.position[pos[crossAxis]]
                }
                ++i
            }
        }


        // <Loop G> Calculate dimensions for absolutely positioned elements
        currentAbsoluteChild = firstAbsoluteChild
        while (currentAbsoluteChild != null) {
            // Pre-fill dimensions when using absolute position and both offsets for
            // the axis are defined (either both left and right or top and bottom).
            ii = 0
            while (ii < 2) {
                axis = if (ii != 0) CSS_FLEX_DIRECTION_ROW else CSS_FLEX_DIRECTION_COLUMN

                if (!Float.isNaN(node.layout.dimensions[dim[axis]]) && !(!Float.isNaN(currentAbsoluteChild.style.dimensions[dim[axis]]) && currentAbsoluteChild.style.dimensions[dim[axis]] >= 0.0) && !Float.isNaN(
                        currentAbsoluteChild.style.position[leading[axis]]
                    ) && !Float.isNaN(currentAbsoluteChild.style.position[trailing[axis]])
                ) {
                    currentAbsoluteChild.layout.dimensions[dim[axis]] = maxOf(
                        boundAxis(
                            currentAbsoluteChild, axis, node.layout.dimensions[dim[axis]] -
                                    (node.style.border.getWithFallback(
                                        leadingSpacing[axis],
                                        leading[axis]
                                    ) + node.style.border.getWithFallback(
                                        trailingSpacing[axis], trailing[axis]
                                    )) -
                                    (currentAbsoluteChild.style.margin.getWithFallback(
                                        leadingSpacing[axis],
                                        leading[axis]
                                    ) + currentAbsoluteChild.style.margin.getWithFallback(
                                        trailingSpacing[axis], trailing[axis]
                                    )) -
                                    (if (Float.isNaN(currentAbsoluteChild.style.position[leading[axis]])) 0f else currentAbsoluteChild.style.position[leading[axis]]) -
                                    (if (Float.isNaN(currentAbsoluteChild.style.position[trailing[axis]])) 0f else currentAbsoluteChild.style.position[trailing[axis]])
                        ),  // You never want to go smaller than padding
                        ((currentAbsoluteChild.style.padding.getWithFallback(
                            leadingSpacing[axis],
                            leading[axis]
                        ) + currentAbsoluteChild.style.border.getWithFallback(
                            leadingSpacing[axis], leading[axis]
                        )) + (currentAbsoluteChild.style.padding.getWithFallback(
                            trailingSpacing[axis], trailing[axis]
                        ) + currentAbsoluteChild.style.border.getWithFallback(
                            trailingSpacing[axis], trailing[axis]
                        )))
                    )
                }

                if (!Float.isNaN(currentAbsoluteChild.style.position[trailing[axis]]) &&
                    !!Float.isNaN(currentAbsoluteChild.style.position[leading[axis]])
                ) {
                    currentAbsoluteChild.layout.position[leading[axis]] =
                        node.layout.dimensions[dim[axis]] -
                                currentAbsoluteChild.layout.dimensions[dim[axis]] -
                                (if (Float.isNaN(currentAbsoluteChild.style.position[trailing[axis]])) 0f else currentAbsoluteChild.style.position[trailing[axis]])
                }
                ii++
            }

            child = currentAbsoluteChild
            currentAbsoluteChild = currentAbsoluteChild.nextAbsoluteChild
            child.nextAbsoluteChild = null
        }
    }
    /** END_GENERATED  */
}
