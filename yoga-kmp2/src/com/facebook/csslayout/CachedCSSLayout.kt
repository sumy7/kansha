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
 * CSSLayout with additional information about the conditions under which it was generated.
 * [.requestedWidth] and [.requestedHeight] are the width and height the parent set on
 * this node before calling layout visited us.
 */
class CachedCSSLayout : CSSLayout() {
    var requestedWidth: Float = com.facebook.csslayout.CSSConstants.UNDEFINED
    var requestedHeight: Float = com.facebook.csslayout.CSSConstants.UNDEFINED
    var parentMaxWidth: Float = com.facebook.csslayout.CSSConstants.UNDEFINED
}
