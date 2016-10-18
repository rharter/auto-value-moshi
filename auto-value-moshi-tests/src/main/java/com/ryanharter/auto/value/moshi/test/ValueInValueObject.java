package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue public abstract class ValueInValueObject {
  public static JsonAdapter<ValueInValueObject> jsonAdapter(Moshi moshi) {
    return AutoValue_ValueInValueObject.jsonAdapter(moshi);
  }

  public abstract Value value();

  @AutoValue public static abstract class Value {
    public static JsonAdapter<Value> jsonAdapter(Moshi moshi) {
      return AutoValue_ValueInValueObject_Value.jsonAdapter(moshi);
    }

    public abstract String a();
  }
}
