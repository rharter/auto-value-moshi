package com.ryanharter.auto.value.moshi.test;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonQualifier;
import com.squareup.moshi.ToJson;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

@JsonQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ReverseList {

  class JsonAdapter {
    @ReverseList @FromJson List<String> fromJson(List<String> input) {
      return reverse(input);
    }

    @ToJson List<String> toJson(@ReverseList List<String> input) {
      return reverse(input);
    }

    private List<String> reverse(@ReverseList List<String> input) {
      List<String> reversed = new ArrayList<>();
      for (int i = input.size() - 1; i >= 0; i--) {
        reversed.add(input.get(i));
      }
      return reversed;
    }
  }
}
