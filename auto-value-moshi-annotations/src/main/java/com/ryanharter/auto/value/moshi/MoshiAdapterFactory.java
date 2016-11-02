package com.ryanharter.auto.value.moshi;

import com.squareup.moshi.JsonAdapter.Factory;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Annotation to indicate that a given class should generate a concrete implementation of a
 * {@link Factory} that handles all the publicly denoted adapter implementations of this
 * project.
 * <p>
 * <code><pre>
 *   &#64;MoshiAdapterFactory
 *   public abstract class Factory implements JsonAdapter.Factory {
 *     public static Factory create() {
 *       return new AutoValueMoshi_Factory();
 *     }
 *   }
 * </pre></code>
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface MoshiAdapterFactory {
}
