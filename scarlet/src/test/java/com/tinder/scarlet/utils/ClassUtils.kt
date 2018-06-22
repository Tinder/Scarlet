/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.utils

import java.lang.reflect.Method

internal fun Class<*>.onlyMethod(): Method {
    check(declaredMethods.size == 1) { "More than one method declared." }
    return declaredMethods[0]
}
