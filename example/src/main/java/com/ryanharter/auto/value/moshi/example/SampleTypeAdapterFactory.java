package com.ryanharter.auto.value.moshi.example;

import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@MoshiAdapterFactory
public abstract class SampleTypeAdapterFactory implements JsonAdapter.Factory {
    public static Moshi.Builder configureMoshiBuilder(Moshi.Builder builder) {
        return builder.add(new AutoValueMoshi_SampleTypeAdapterFactory()).add(new BirthDateAdapter());
    }
}
