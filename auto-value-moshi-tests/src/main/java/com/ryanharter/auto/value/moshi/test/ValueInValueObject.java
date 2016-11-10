package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue public abstract class ValueInValueObject {
  public static JsonAdapter<ValueInValueObject> jsonAdapter(Moshi moshi) {
    return new AutoValue_ValueInValueObject.MoshiJsonAdapter(moshi);
  }

  public abstract Value value();

  @AutoValue public abstract static class Value {
    public static JsonAdapter<Value> jsonAdapter(Moshi moshi) {
      return new AutoValue_ValueInValueObject_Value.MoshiJsonAdapter(moshi);
    }

    public abstract String a();
  }
}
