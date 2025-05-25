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
 * Class representing CSS spacing (padding, margin, and borders). This is mostly necessary to
 * properly implement interactions and updates for properties like margin, marginLeft, and
 * marginHorizontal.
 */
class Spacing {
    private val mSpacing = newFullSpacingArray()

    private var mDefaultSpacing: FloatArray? = null
    private var mValueFlags = 0
    private var mHasAliasesSet = false

    /**
     * Set a spacing value.
     *
     * @param spacingType one of [.LEFT], [.TOP], [.RIGHT], [.BOTTOM],
     * [.VERTICAL], [.HORIZONTAL], [.ALL]
     * @param value the value for this direction
     * @return `true` if the spacing has changed, or `false` if the same value was already
     * set
     */
    fun set(spacingType: Int, value: Float): Boolean {
        if (!FloatUtil.floatsEqual(mSpacing[spacingType], value)) {
            mSpacing[spacingType] = value

            if (CSSConstants.isUndefined(value)) {
                mValueFlags = mValueFlags and sFlagsMap[spacingType].inv()
            } else {
                mValueFlags = mValueFlags or sFlagsMap[spacingType]
            }

            mHasAliasesSet =
                (mValueFlags and sFlagsMap[ALL]) != 0 || (mValueFlags and sFlagsMap[VERTICAL]) != 0 || (mValueFlags and sFlagsMap[HORIZONTAL]) != 0

            return true
        }
        return false
    }

    /**
     * Set a default spacing value. This is used as a fallback when no spacing has been set for a
     * particular direction.
     *
     * @param spacingType one of [.LEFT], [.TOP], [.RIGHT], [.BOTTOM]
     * @param value the default value for this direction
     * @return
     */
    fun setDefault(spacingType: Int, value: Float): Boolean {
        if (mDefaultSpacing == null) {
            mDefaultSpacing = newSpacingResultArray()
        }
        if (!FloatUtil.floatsEqual(mDefaultSpacing!![spacingType], value)) {
            mDefaultSpacing!![spacingType] = value
            return true
        }
        return false
    }

    /**
     * Get the spacing for a direction. This takes into account any default values that have been set.
     *
     * @param spacingType one of [.LEFT], [.TOP], [.RIGHT], [.BOTTOM]
     */
    fun get(spacingType: Int): Float {
        val defaultValue = if (mDefaultSpacing != null)
            mDefaultSpacing!![spacingType]
        else
            (if (spacingType == START || spacingType == END) CSSConstants.UNDEFINED else 0f)

        if (mValueFlags == 0) {
            return defaultValue
        }

        if ((mValueFlags and sFlagsMap[spacingType]) != 0) {
            return mSpacing[spacingType]
        }

        if (mHasAliasesSet) {
            val secondType = if (spacingType == TOP || spacingType == BOTTOM) VERTICAL else HORIZONTAL
            if ((mValueFlags and sFlagsMap[secondType]) != 0) {
                return mSpacing[secondType]
            } else if ((mValueFlags and sFlagsMap[ALL]) != 0) {
                return mSpacing[ALL]
            }
        }

        return defaultValue
    }

    /**
     * Get the raw value (that was set using [.set]), without taking into account
     * any default values.
     *
     * @param spacingType one of [.LEFT], [.TOP], [.RIGHT], [.BOTTOM],
     * [.VERTICAL], [.HORIZONTAL], [.ALL]
     */
    fun getRaw(spacingType: Int): Float {
        return mSpacing[spacingType]
    }

    /**
     * Resets the spacing instance to its default state. This method is meant to be used when
     * recycling [Spacing] instances.
     */
    fun reset() {
        mSpacing.fill(CSSConstants.UNDEFINED)
        mDefaultSpacing = null
        mHasAliasesSet = false
        mValueFlags = 0
    }

    /**
     * Try to get start value and fallback to given type if not defined. This is used privately
     * by the layout engine as a more efficient way to fetch direction-aware values by
     * avoid extra method invocations.
     */
    fun getWithFallback(spacingType: Int, fallbackType: Int): Float {
        return if ((mValueFlags and sFlagsMap[spacingType]) != 0)
            mSpacing[spacingType]
        else
            get(fallbackType)
    }

    companion object {
        /**
         * Spacing type that represents the left direction. E.g. `marginLeft`.
         */
        const val LEFT: Int = 0

        /**
         * Spacing type that represents the top direction. E.g. `marginTop`.
         */
        const val TOP: Int = 1

        /**
         * Spacing type that represents the right direction. E.g. `marginRight`.
         */
        const val RIGHT: Int = 2

        /**
         * Spacing type that represents the bottom direction. E.g. `marginBottom`.
         */
        const val BOTTOM: Int = 3

        /**
         * Spacing type that represents vertical direction (top and bottom). E.g. `marginVertical`.
         */
        const val VERTICAL: Int = 4

        /**
         * Spacing type that represents horizontal direction (left and right). E.g.
         * `marginHorizontal`.
         */
        const val HORIZONTAL: Int = 5

        /**
         * Spacing type that represents start direction e.g. left in left-to-right, right in right-to-left.
         */
        const val START: Int = 6

        /**
         * Spacing type that represents end direction e.g. right in left-to-right, left in right-to-left.
         */
        const val END: Int = 7

        /**
         * Spacing type that represents all directions (left, top, right, bottom). E.g. `margin`.
         */
        const val ALL: Int = 8

        private val sFlagsMap = intArrayOf(
            1,  /*LEFT*/
            2,  /*TOP*/
            4,  /*RIGHT*/
            8,  /*BOTTOM*/
            16,  /*VERTICAL*/
            32,  /*HORIZONTAL*/
            64,  /*START*/
            128,  /*END*/
            256,  /*ALL*/
        )

        private fun newFullSpacingArray(): FloatArray {
            return floatArrayOf(
                CSSConstants.UNDEFINED,
                CSSConstants.UNDEFINED,
                CSSConstants.UNDEFINED,
                CSSConstants.UNDEFINED,
                CSSConstants.UNDEFINED,
                CSSConstants.UNDEFINED,
                CSSConstants.UNDEFINED,
                CSSConstants.UNDEFINED,
                CSSConstants.UNDEFINED,
            )
        }

        private fun newSpacingResultArray(defaultValue: Float = 0f): FloatArray {
            return floatArrayOf(
                defaultValue,
                defaultValue,
                defaultValue,
                defaultValue,
                defaultValue,
                defaultValue,
                CSSConstants.UNDEFINED,
                CSSConstants.UNDEFINED,
                defaultValue,
            )
        }
    }
}
