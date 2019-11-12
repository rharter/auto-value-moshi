package com.ryanharter.auto.value.moshi.example;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.moshi.AutoValueMoshiBuilder;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class Address {

    public static Address create(String streetName, String city) {
        return new AutoValue_Address(streetName, city);
    }

    public static JsonAdapter<Address> jsonAdapter(Moshi moshi) {
        return new AutoValue_Address.MoshiJsonAdapter(moshi);
    }

    @Json(name = "street-name")
    public abstract String streetName();

    public abstract String city();

    // This builder should never be invoked
    public static Builder badBuilder() {
        throw new RuntimeException("Wrong builder instance used!");
    }

    @AutoValueMoshiBuilder
    public static Builder builder() {
        return new AutoValue_Address.Builder();
    }

    @AutoValue.Builder
    interface Builder {
        Builder streetName(String streetName);
        Builder city(String city);
        Address build();
    }
}
