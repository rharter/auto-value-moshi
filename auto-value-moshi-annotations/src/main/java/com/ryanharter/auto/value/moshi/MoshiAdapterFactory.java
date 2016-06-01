package com.ryanharter.auto.value.moshi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by RobX on 16/5/31.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface MoshiAdapterFactory {
  String packageName() default "com.ryanharter.auto.value.moshi";

  String simpleName() default "AutoValueMoshiAdapterFactory";
}
