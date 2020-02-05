package com.ryanharter.auto.value.moshi;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * If present, indicates that the annotated method should be used for retrieving an instance of the
 * AutoValue.Builder. Only necessary if there is more than one builder method.
 */
@Retention(CLASS)
@Target(METHOD)
public @interface AutoValueMoshiBuilder {

  /**
   * If a property has multiple setters, this annotation can be used to indicate which setter
   * method should be used by AutoValueMoshi.
   *
   * <pre><code>
   *   &#064;AutoValue.Builder
   *   interface Builder {
   *     &#064;AutoValueMoshiBuilder.Builder
   *     Builder setFoo(List&lt;String&gt; number);
   *     Builder setFoo(Collection&lt;String&gt; number);
   *   }
   * </code></pre>
   */
  @Retention(CLASS)
  @Target(METHOD)
  @interface Setter {

  }
}
