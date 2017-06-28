package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.lang.reflect.Type;
import java.util.List;

@AutoValue public abstract class ObjectWithGenericsAndQualifiers<T> {
  @Json(name = "reversed_list") @ReverseList public abstract List<T> reversedList();
  @ReverseString public abstract T reversedGeneric();
  @ReverseString public abstract String reversedString();
  public abstract String normalString();

  public static <T> JsonAdapter<ObjectWithGenericsAndQualifiers<T>> jsonAdapter(Moshi moshi,
      Type[] types) {

    return new AutoValue_ObjectWithGenericsAndQualifiers.MoshiJsonAdapter(moshi, types);
  }
}
