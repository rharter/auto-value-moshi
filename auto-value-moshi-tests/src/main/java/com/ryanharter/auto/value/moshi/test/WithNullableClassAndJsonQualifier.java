package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

import javax.annotation.Nullable;

@AutoValue
public abstract class WithNullableClassAndJsonQualifier {
    public abstract String value();

    @Nullable @ReverseList public abstract List<String> list();

    public static JsonAdapter<WithNullableClassAndJsonQualifier> jsonAdapter(Moshi moshi) {
        return new AutoValue_WithNullableClassAndJsonQualifier.MoshiJsonAdapter(moshi);
    }
}
