package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.List;

@AutoValue public abstract class WithJsonQualifierAnnotationsObject {
  public static JsonAdapter<WithJsonQualifierAnnotationsObject> jsonAdapter(Moshi moshi) {
    return AutoValue_WithJsonQualifierAnnotationsObject.jsonAdapter(moshi);
  }

  @Json(name = "key1") public abstract String a();

  @ReverseList public abstract List<String> b();
}
