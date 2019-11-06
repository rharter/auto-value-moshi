package com.ryanharter.auto.value.moshi.example;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Date;

@AutoValue
public abstract class Person {
    public abstract String name();

    public abstract int gender();

    public abstract int age();

    public abstract Date birthdate();

    @Nullable
    public abstract Address address();

    public static Builder builder() {
        return new AutoValue_Person.Builder();
    }

    public static JsonAdapter<Person> jsonAdapter(Moshi moshi) {
        return new AutoValue_Person.MoshiJsonAdapter(moshi);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);

        public abstract Builder gender(int gender);

        public abstract Builder age(int age);

        public abstract Builder birthdate(Date birthdate);

        public abstract Builder address(Address address);

        public abstract Person build();
    }

    public @interface Nullable { }
}
