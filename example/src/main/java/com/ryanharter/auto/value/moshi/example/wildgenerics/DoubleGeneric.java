package com.ryanharter.auto.value.moshi.example.wildgenerics;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonClass;
import java.util.List;

@JsonClass(generateAdapter = true, generator = "avm")
@AutoValue
public abstract class DoubleGeneric<T> extends GenericBase<T> {

  public abstract String topLevelNonGeneric();

  public abstract T topLevelGeneric();

  public abstract List<T> topLevelGenericCollection();

  public static <T> Builder<T> builder() {
    return new AutoValue_DoubleGeneric.Builder<T>();
  }

  @AutoValue.Builder public abstract static class Builder<T>
      implements GenericBase.Builder<T, Builder<T>> {
    public abstract Builder<T> topLevelNonGeneric(String topLevel);

    public abstract Builder<T> topLevelGeneric(T topLevel);

    public abstract Builder<T> topLevelGenericCollection(List<T> topLevel);

    public abstract DoubleGeneric<T> build();
  }
}
