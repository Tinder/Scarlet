/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.ws;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Returns an infinite {@link com.tinder.scarlet.Stream} of {@link com.tinder.scarlet.WebSocket.Event} or
 * {@link com.tinder.scarlet.Message}.
 *
 * Note: this is declared as a Java annotation because Scarlet uses java reflection API which can
 * not access Kotlin annotations.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Receive {

}
