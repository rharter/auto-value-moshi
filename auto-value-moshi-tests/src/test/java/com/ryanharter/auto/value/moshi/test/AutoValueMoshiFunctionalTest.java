package com.ryanharter.auto.value.moshi.test;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public final class AutoValueMoshiFunctionalTest {
  private final Moshi moshi = new Moshi.Builder()
      .add(FunctionalTestsAdapterFactory.create())
      .add(new ReverseList.JsonAdapter())
      .build();

  @Test public void standardObject() throws Exception {
    JsonAdapter<StandardObject> adapter = moshi.adapter(StandardObject.class);

    StandardObject fromJson = adapter.fromJson("{\n"
        + "  \"aBoolean\": true,\n"
        + "  \"aByte\": 1,\n"
        + "  \"aChar\": \"a\",\n"
        + "  \"aDouble\": 1.0,\n"
        + "  \"aFloat\": 1.2,\n"
        + "  \"aInt\": 2,\n"
        + "  \"aLong\": -9223372036854775808,\n"
        + "  \"aShort\": 3,\n"
        + "  \"aBooleanObj\": false,\n"
        + "  \"aByteObj\": 128,\n"
        + "  \"aCharacterObj\": \"c\",\n"
        + "  \"aDoubleObj\": 1e0,\n"
        + "  \"aFloatObj\": 3.0,\n"
        + "  \"aIntegerObj\": 42,\n"
        + "  \"aLongObj\": 9223372036854775807,\n"
        + "  \"aShortObj\": 5,\n"
        + "  \"aString\": \"A string\",\n"
        + "  \"aList\": [\n"
        + "    \"one\",\n"
        + "    \"two\",\n"
        + "    \"three\"\n"
        + "  ],\n"
        + "  \"aMap\": {\n"
        + "    \"one\": 1,\n"
        + "    \"two\": 2,\n"
        + "    \"three\": 3\n"
        + "  },\n"
        + "  \"anEnum\": \"VALUE_1\",\n"
        + "  \"anArray\": [1, 2, 3, 5, 8, 13, 21]\n"
        + "}");

    assertThat(fromJson.aBoolean()).isTrue();
    assertThat(fromJson.aByte()).isEqualTo((byte) 1);
    assertThat(fromJson.aChar()).isEqualTo('a');
    assertThat(fromJson.aDouble()).isEqualTo(1.0);
    assertThat(fromJson.aFloat()).isEqualTo(1.2f);
    assertThat(fromJson.aInt()).isEqualTo(2);
    assertThat(fromJson.aLong()).isEqualTo(Long.MIN_VALUE);
    assertThat(fromJson.aShort()).isEqualTo((short) 3);
    assertThat(fromJson.aBooleanObj()).isFalse();
    assertThat(fromJson.aByteObj()).isEqualTo((byte) 128);
    assertThat(fromJson.aCharacterObj()).isEqualTo('c');
    assertThat(fromJson.aDoubleObj()).isEqualTo(1.0);
    assertThat(fromJson.aFloatObj()).isEqualTo(3.0f);
    assertThat(fromJson.aIntegerObj()).isEqualTo(42);
    assertThat(fromJson.aLongObj()).isEqualTo(Long.MAX_VALUE);
    assertThat(fromJson.aShortObj()).isEqualTo((short) 5);
    assertThat(fromJson.aString()).isEqualTo("A string");
    assertThat(fromJson.aList()).containsExactly("one", "two", "three");
    assertThat(fromJson.aMap()).containsExactly(
        entry("one", 1), entry("two", 2), entry("three", 3));
    assertThat(fromJson.anEnum()).isEqualTo(StandardObject.AnEnum.VALUE_1);
    assertThat(fromJson.anArray()).containsExactly(1, 2, 3, 5, 8, 13, 21);

    String toJson = adapter.toJson(fromJson);
    assertThat(toJson).isEqualTo("{\"aBoolean\":true,"
        + "\"aByte\":1,"
        + "\"aChar\":\"a\","
        + "\"aDouble\":1.0,"
        + "\"aFloat\":1.2,"
        + "\"aInt\":2,"
        + "\"aLong\":-9223372036854775808,"
        + "\"aShort\":3,"
        + "\"aBooleanObj\":false,"
        + "\"aByteObj\":128,"
        + "\"aCharacterObj\":\"c\","
        + "\"aDoubleObj\":1.0,"
        + "\"aFloatObj\":3.0,"
        + "\"aIntegerObj\":42,"
        + "\"aLongObj\":9223372036854775807,"
        + "\"aShortObj\":5,"
        + "\"aString\":\"A string\","
        + "\"aList\":[\"one\",\"two\",\"three\"],"
        + "\"aMap\":{\"one\":1,\"two\":2,\"three\":3},"
        + "\"anEnum\":\"VALUE_1\","
        + "\"anArray\":[1,2,3,5,8,13,21]}");
  }

  @Test public void objectWithJsonQualifierAnnotations() throws Exception {
    JsonAdapter<WithJsonQualifierAnnotationsObject> adapter =
        moshi.adapter(WithJsonQualifierAnnotationsObject.class);

    WithJsonQualifierAnnotationsObject fromJson = adapter.fromJson("{\n"
        + "  \"key1\": \"A Value\",\n"
        + "  \"b\": [\n"
        + "    \"one\",\n"
        + "    \"two\",\n"
        + "    \"three\"\n"
        + "  ]\n"
        + "}");

    assertThat(fromJson.a()).isEqualTo("A Value");
    assertThat(fromJson.b()).containsExactly("three", "two", "one");

    String toJson = adapter.toJson(fromJson);
    assertThat(toJson).isEqualTo("{\"key1\":\"A Value\",\"b\":[\"one\",\"two\",\"three\"]}");
  }

  @Test public void valueInValueObject() throws Exception {
    JsonAdapter<ValueInValueObject> adapter = moshi.adapter(ValueInValueObject.class);

    ValueInValueObject fromJson = adapter.fromJson("{\n"
        + "  \"value\": {\n"
        + "    \"a\": \"Some value\"\n"
        + "  }\n"
        + "}");

    assertThat(fromJson.value().a()).isEqualTo("Some value");

    String toJson = adapter.toJson(fromJson);
    assertThat(toJson).isEqualTo("{\"value\":{\"a\":\"Some value\"}}");
  }
}
