package com.ryanharter.auto.value.moshi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Sets the default value for an int field if not present in deserialized JSON.
 */
@Target({METHOD})
@Retention(RUNTIME)
@Documented
public @interface DefaultInt {
  int value();
}
