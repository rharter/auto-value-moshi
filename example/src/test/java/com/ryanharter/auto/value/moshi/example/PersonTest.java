package com.ryanharter.auto.value.moshi.example;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Test;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class PersonTest {
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
}

