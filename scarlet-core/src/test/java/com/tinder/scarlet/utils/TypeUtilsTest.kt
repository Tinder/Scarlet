/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type

@RunWith(Enclosed::class)
internal class TypeUtilsTest {

    @RunWith(Parameterized::class)
    class GetRawType(
        private val type: Type,
        private val expectedRawType: Type
    ) {

        @Test
        fun test() {
            assertThat(type.getRawType()).isSameAs(expectedRawType)
        }

        companion object {
            @Parameterized.Parameters(name = "{index}: TypeUtils.getRawType(type = {0}) = {1}")
            @JvmStatic
            fun data() = listOf(
                param(type = getReturnType { string() }, expectedRawType = String::class.java),
                param(type = getReturnType { listOfString() }, expectedRawType = List::class.java),
                param(type = getReturnType { arrayOfString() }, expectedRawType = Array<String>::class.java),
                param(type = getReturnType { generic() }, expectedRawType = Any::class.java),
                param(type = getReturnType { genericWithUpperBound() }, expectedRawType = Any::class.java)
            )

            private fun param(type: Type, expectedRawType: Type) = arrayOf(type, expectedRawType)
        }
    }

    @RunWith(Parameterized::class)
    class HasUnresolvableType(
        private val type: Type,
        private val expectedHasUnresolvableType: Boolean
    ) {

        @Test
        fun test() {
            assertThat(type.hasUnresolvableType()).isSameAs(expectedHasUnresolvableType)
        }

        companion object {
            @Parameterized.Parameters(name = "{index}: TypeUtils.hasUnresolvableType(type = {0}) = {1}")
            @JvmStatic
            fun data() = listOf(
                param(type = getReturnType { string() }, expectedHasUnresolvableType = false),
                param(type = getReturnType { listOfString() }, expectedHasUnresolvableType = false),
                param(type = getReturnType { arrayOfString() }, expectedHasUnresolvableType = false),
                param(type = getReturnType { generic() }, expectedHasUnresolvableType = true),
                param(type = getReturnType { genericWithUpperBound() }, expectedHasUnresolvableType = true),
                param(type = getReturnType { arrayOfGeneric<Any>() }, expectedHasUnresolvableType = true),
                param(type = getReturnType { listOfGeneric<Any>() }, expectedHasUnresolvableType = true)
            )

            private fun param(type: Type, expectedHasUnresolvableType: Boolean) =
                arrayOf(type, expectedHasUnresolvableType)
        }
    }

    @RunWith(Parameterized::class)
    class GetParameterUpperBound(
        private val type: ParameterizedType,
        private val expectedParameterUpperBounds: Array<Type>
    ) {

        @Test
        fun test() {
            val parameterUpperBound = (0 until type.actualTypeArguments.size).map { type.getParameterUpperBound(it) }
            assertThat(parameterUpperBound).containsExactly(*expectedParameterUpperBounds)
        }

        companion object {
            @Parameterized.Parameters(name = "{index}: TypeUtils.getParameterUpperBound")
            @JvmStatic
            fun data() = listOf(
                param(
                    type = getReturnType { listOfString() },
                    expectedParameterUpperBound = arrayOf(String::class.java)
                ),
                param(
                    type = getReturnType { pairOfStringAndNumber() },
                    expectedParameterUpperBound = arrayOf(String::class.java, Number::class.java)
                )
            )

            private fun param(type: ParameterizedType, expectedParameterUpperBound: Array<Type>) =
                arrayOf(type, expectedParameterUpperBound)
        }
    }

    private companion object {
        @Suppress("UNUSED")
        private interface Types {
            fun string(): String

            fun arrayOfString(): Array<String>

            fun listOfString(): List<String>

            fun pairOfStringAndNumber(): Pair<String, Number>

            fun <T> generic(): T

            fun <T : Type> genericWithUpperBound(): T

            fun <T> arrayOfGeneric(): Array<T>

            fun <T> listOfGeneric(): List<T>
        }

        private inline fun <reified T : Type> getReturnType(noinline methodCall: Types.() -> Any) =
            ReturnTypeResolver<T>().resolve(methodCall)

        private class ReturnTypeResolver<out T : Type> {
            private lateinit var method: Method

            private val methodRecorder = Proxy.newProxyInstance(
                Types::class.java.classLoader,
                arrayOf(Types::class.java)
            ) { _, method, _ ->
                this.method = method
                null
            } as Types

            fun resolve(methodCall: Types.() -> Any): T {
                methodRecorder.methodCall()
                @Suppress("UNCHECKED_CAST")
                return method.genericReturnType as T
            }
        }
    }
}
