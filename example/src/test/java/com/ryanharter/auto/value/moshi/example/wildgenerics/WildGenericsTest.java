package com.ryanharter.auto.value.moshi.example.wildgenerics;

import com.ryanharter.auto.value.moshi.example.Address;
import com.ryanharter.auto.value.moshi.example.SampleJsonAdapterFactory;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.reflect.Type;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public final class WildGenericsTest {

  @Test public void addressGenericBase() throws IOException {
    //language=JSON
    String json = "{\"generic\":{\"street-name\":\"genericStreet\",\"city\":\"genericCity\"},"
        + "\"notGeneric\":\"notGeneric\",\"collection\":[{\"street-name\":\"fooStreet\","
        + "\"city\":\"barCity\"}],\"topLevelNonGeneric\":\"topLevelNonGeneric\"}";

    Moshi moshi = new Moshi.Builder()
        .add(SampleJsonAdapterFactory.create())
        .build();
    JsonAdapter<AddressGenericBase> adapter = moshi.adapter(AddressGenericBase.class);
    AddressGenericBase instance = adapter.fromJson(json);

    AddressGenericBase testInstance = AddressGenericBase.builder()
        .topLevelNonGeneric("topLevelNonGeneric")
        .collection(singletonList(Address.create("fooStreet", "barCity")))
        .generic(Address.create("genericStreet", "genericCity"))
        .notGeneric("notGeneric")
        .build();

    assertEquals(instance, testInstance);
    assertEquals(json, adapter.toJson(testInstance));
  }

  @Test public void doubleGeneric() throws IOException {
    //language=JSON
    String json = "{\"generic\":{\"street-name\":\"genericStreet\",\"city\":\"genericCity\"},"
        + "\"notGeneric\":\"notGeneric\",\"collection\":[{\"street-name\":\"fooStreet\","
        + "\"city\":\"barCity\"}],\"topLevelNonGeneric\":\"topLevelNonGeneric\","
        + "\"topLevelGeneric\":{\"street-name\":\"topStreet\",\"city\":\"topCity\"},"
        + "\"topLevelGenericCollection\":[{\"street-name\":\"topCollectionStreet\","
        + "\"city\":\"topCollectionCity\"}]}";

    Moshi moshi = new Moshi.Builder()
        .add(SampleJsonAdapterFactory.create())
        .build();
    Type token = Types.newParameterizedType(DoubleGeneric.class, Address.class);
    JsonAdapter<DoubleGeneric<Address>> adapter = moshi.adapter(token);
    DoubleGeneric<Address> instance = adapter.fromJson(json);

    DoubleGeneric<Address> testInstance =
        DoubleGeneric.<Address>builder().topLevelNonGeneric("topLevelNonGeneric")
            .topLevelGeneric(Address.create("topStreet", "topCity"))
            .topLevelGenericCollection(singletonList(Address.create("topCollectionStreet",
                "topCollectionCity")))
            .collection(singletonList(Address.create("fooStreet", "barCity")))
            .generic(Address.create("genericStreet", "genericCity"))
            .notGeneric("notGeneric")
            .build();

    assertEquals(instance, testInstance);
    assertEquals(json, adapter.toJson(testInstance));
  }
}
