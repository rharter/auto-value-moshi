package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "avm")
@AutoValue
public abstract class NativeMoshiClass {
  public abstract String property();

  @AutoValue.Builder
  public interface Builder {
    Builder property(String prop);
    NativeMoshiClass build();
  }

  @JsonClass(generateAdapter = true, generator = "avm")
  @AutoValue
  public abstract static class Nested {
    public abstract String property();

    @AutoValue.Builder
    public interface Builder {
      Builder property(String prop);
      Nested build();
    }
  }
}
