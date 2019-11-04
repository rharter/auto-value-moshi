package com.ryanharter.auto.value.moshi.example;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class PersonTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testMoshi() throws Exception {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Moshi moshi = new Moshi.Builder()
                .add(SampleTypeAdapterFactory.create())
                .build();
        Person person = Person.builder()
                .name("Piasy")
                .gender(1)
                .age(23)
                .address(Address.create("street", "city"))
                .build();
        String json = "{\"name\":\"Piasy\",\"gender\":1,"
                + "\"age\":23,\"address\":{\"street-name\":\"street\",\"city\":\"city\"}}";

        JsonAdapter<Person> jsonAdapter = moshi.adapter(Person.class);
        String toJson = jsonAdapter.toJson(person);
        assertEquals(json, toJson);

        Person fromJson = jsonAdapter.fromJson(json);
        assertEquals("Piasy", fromJson.name());
        assertEquals(23, fromJson.age());
        assertEquals(1, fromJson.gender());
    }

    @Test
    public void testMoshiWithValidation() throws IOException {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("age cannot be negative");

        Moshi moshi = new Moshi.Builder()
                .add(SampleTypeAdapterFactory.create())
                .build();

        //language=json
        String json = "{\"name\":\"Piasy\",\"gender\":1,\"age\":-1,\"birthdate\":\"2007-11-11\",\"address\":{\"street-name\":\"street\",\"city\":\"city\"}}";
        moshi.adapter(Person.class).fromJson(json);
    }

    @Test
    public void testGsonWithDefaults() throws IOException {
        Moshi moshi = new Moshi.Builder()
                .add(SampleTypeAdapterFactory.create())
                .build();

        // "name" and "gender" are unspecified. Should default to "Jane Doe" and 23
        //language=json
        String json = "{\"age\":23,\"birthdate\":\"2007-11-11\",\"address\":{\"street-name\":\"street\",\"city\":\"city\"}}";
        Person fromJson = moshi.adapter(Person.class).fromJson(json);
        assertEquals("Jane Doe", fromJson.name());
        assertEquals(23, fromJson.age());
        assertEquals(0, fromJson.gender());
    }
}

