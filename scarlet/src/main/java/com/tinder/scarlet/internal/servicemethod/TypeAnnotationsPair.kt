/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import java.lang.reflect.Type
import java.util.Arrays

internal data class TypeAnnotationsPair(val type: Type, val annotations: Array<Annotation>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeAnnotationsPair

        if (type != other.type) return false
        if (!Arrays.equals(annotations, other.annotations)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + Arrays.hashCode(annotations)
        return result
    }
}
