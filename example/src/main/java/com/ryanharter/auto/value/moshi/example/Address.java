package com.ryanharter.auto.value.moshi.example;

import com.google.auto.value.AutoValue;
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
}
