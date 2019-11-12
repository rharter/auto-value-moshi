package com.ryanharter.auto.value.moshi.example;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.lang.reflect.Type;

@AutoValue public abstract class GenericsExample<A, B, C> {

    public abstract A a();
    public abstract B b();
    public abstract C c();

    @AutoValue.Builder
    public interface Builder<A, B, C> {
        Builder<A, B, C> a(A a);
        Builder<A, B, C> b(B b);
        Builder<A, B, C> c(C c);
        GenericsExample<A, B, C> build();
    }

    public static <A, B, C> Builder<A, B, C> builder() {
        return new AutoValue_GenericsExample.Builder<A, B, C>();
    }

    public static <A, B, C> JsonAdapter<GenericsExample<A, B, C>> jsonAdapter(Moshi moshi, Type[] types) {
        return new AutoValue_GenericsExample.MoshiJsonAdapter(moshi, types);
    }
}
