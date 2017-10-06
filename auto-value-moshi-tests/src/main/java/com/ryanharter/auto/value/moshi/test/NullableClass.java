package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import javax.annotation.Nullable;

@AutoValue
public abstract class NullableClass {
    @Nullable
    public abstract String string();

    public static JsonAdapter<NullableClass> jsonAdapter(Moshi moshi) {
        return new AutoValue_NullableClass.MoshiJsonAdapter(moshi);
    }
}
