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
 * A context for holding values local to a given instance of layout computation.
 *
 * This is necessary for making layout thread-safe. A separate instance should
 * be used when [CSSNode.calculateLayout] is called concurrently on
 * different node hierarchies.
 */
class CSSLayoutContext {
    /*package*/
    val measureOutput: MeasureOutput = MeasureOutput()
}
