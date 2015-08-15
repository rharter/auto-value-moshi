package com.ryanharter.auto.value.moshi;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by rharter on 8/15/15.
 */
public class AutoValueMoshiJsonAdapterFactory implements JsonAdapter.Factory {

  public static final Set<JsonAdapter.Factory> FACTORIES = Collections.synchronizedSet(new HashSet<>());

  public static void register(JsonAdapter.Factory factory) {
    FACTORIES.add(factory);
  }

  @Override
  public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
    Iterator<JsonAdapter.Factory> it = FACTORIES.iterator();
    while (it.hasNext()) {
      JsonAdapter adapter = it.next().create(type, annotations, moshi);
      if (adapter != null) return adapter;
    }
    return null;
  }
}
