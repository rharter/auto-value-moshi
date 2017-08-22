package com.ryanharter.auto.value.moshi.test;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonQualifier;
import com.squareup.moshi.ToJson;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@JsonQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ReverseString {

  class JsonAdapter {
    @ReverseString @FromJson String fromJson(String input) {
      return reverse(input);
    }

    @ToJson String toJson(@ReverseString String input) {
      return reverse(input);
    }

    private String reverse(@ReverseString String input) {
      StringBuilder sb = new StringBuilder();
      char[] str = input.toCharArray();
      for (int i = str.length - 1; i >= 0; i--) {
        sb.append(str[i]);
      }

      return sb.toString();
    }
  }
}

