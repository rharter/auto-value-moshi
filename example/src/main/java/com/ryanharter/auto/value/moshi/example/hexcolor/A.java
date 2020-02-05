package com.ryanharter.auto.value.moshi.example.hexcolor;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonClass;

// Regression test of https://github.com/rharter/auto-value-moshi/issues/168, also example from
// Moshi directly.
@JsonClass(generateAdapter = true, generator = "avm")
@AutoValue
public abstract class A {
  @HexColor abstract int color();
}