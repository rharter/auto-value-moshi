package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "avm")
@AutoValue
public abstract class GenericNativeMoshiClass<T> {
  public abstract T property();

  @AutoValue.Builder
  public interface Builder<T> {
    Builder<T> property(T prop);
    GenericNativeMoshiClass<T> build();
  }

  @JsonClass(generateAdapter = true, generator = "avm")
  @AutoValue
  public abstract static class Nested<T> {
    public abstract T property();

    @AutoValue.Builder
    public interface Builder<T> {
      Builder<T> property(T prop);
      Nested<T> build();
    }
  }
}
