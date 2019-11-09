package com.ryanharter.auto.value.moshi.example;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue public abstract class User {
    abstract String firstname();
    abstract String lastname();

    public static JsonAdapter<User> jsonAdapter(Moshi moshi) {
        return new AutoValue_User.MoshiJsonAdapter(moshi);
    }

    public static User with(String firstname, String lastname) {
        return new AutoValue_User(firstname, lastname);
    }
}
