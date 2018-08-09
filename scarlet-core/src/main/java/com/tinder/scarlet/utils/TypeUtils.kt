/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("TypeUtils")

package com.tinder.scarlet.utils

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

fun Type.getRawType(): Class<*> = Utils.getRawType(this)

fun Type.hasUnresolvableType(): Boolean = Utils.hasUnresolvableType(this)

fun ParameterizedType.getParameterUpperBound(index: Int): Type =
    Utils.getParameterUpperBound(index, this)
