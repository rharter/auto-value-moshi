package com.ryanharter.auto.value.moshi.example;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class WebResponse<T> {
    public abstract int status();
    public abstract T data();
    public abstract List<T> dataList();
    public abstract Map<String, List<T>> dataMap();

    public static <T> JsonAdapter<WebResponse<T>> jsonAdapter(Moshi moshi, Type[] types) {
        return new AutoValue_WebResponse.MoshiJsonAdapter(moshi, types);
    }
}

