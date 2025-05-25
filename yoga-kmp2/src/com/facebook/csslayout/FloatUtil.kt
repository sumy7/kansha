/**
 * Copyright (c) 2014, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.csslayout

import kotlin.math.abs

object FloatUtil {
    private const val EPSILON = .00001f

    fun isNaN(value: Float) = value.isNaN()

    fun floatsEqual(f1: Float, f2: Float): Boolean {
        if (f1.isNaN() || f2.isNaN()) {
            return f1.isNaN() && f2.isNaN()
        }
        return abs(f2 - f1) < EPSILON
    }
}
