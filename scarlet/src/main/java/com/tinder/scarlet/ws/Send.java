/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.ws;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Attempts to enqueue an outgoing {@link com.tinder.scarlet.Message}.
 * <p>
 * Note: this is declared as a Java annotation because Scarlet uses java reflection API which can
 * not access Kotlin annotations.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Send {

}
