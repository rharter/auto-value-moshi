package com.ryanharter.auto.value.moshi.test;

import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;
import com.squareup.moshi.JsonAdapter;

@MoshiAdapterFactory
public abstract class FunctionalTestsAdapterFactory implements JsonAdapter.Factory {
  public static JsonAdapter.Factory create() {
    return new AutoValueMoshi_FunctionalTestsAdapterFactory();
  }
}
