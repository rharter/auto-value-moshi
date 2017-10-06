package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import javax.annotation.Nullable;

@AutoValue
public abstract class WithNullableClass {
    public abstract String value();

    @Nullable public abstract NullableClass nullableClass();

    public static JsonAdapter<WithNullableClass> jsonAdapter(Moshi moshi) {
        return new AutoValue_WithNullableClass.MoshiJsonAdapter(moshi);
    }
}
