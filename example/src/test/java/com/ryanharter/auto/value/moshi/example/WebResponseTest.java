package com.ryanharter.auto.value.moshi.example;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;

import static junit.framework.TestCase.assertEquals;

public class WebResponseTest {
    Moshi moshi = new Moshi.Builder()
            .add(SampleJsonAdapterFactory.create())
            .build();

    @Test
    public void handlesBasicTypes() throws IOException {
        String json = "{\"status\":200,\"data\":\"string\","
                + "\"dataList\":[\"string\"],"
                + "\"dataMap\":{\"key\":[\"string\"]}}";
        Type webresponseType = Types.newParameterizedType(WebResponse.class, String.class);
        JsonAdapter<WebResponse<String>> jsonAdapter = moshi.adapter(webresponseType);
        WebResponse<String> response = jsonAdapter.fromJson(json);

        assertEquals("string", response.data());
        assertEquals("string", response.dataList().get(0));
        assertEquals("string", response.dataMap().get("key").get(0));
        assertEquals(200, response.status());
    }

    @Test public void handlesComplexTypes() throws IOException {
        String json = "{\"status\":200,\"data\":{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}, "
                + "\"dataList\":[{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}],"
                + "\"dataMap\":{\"key\":[{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}]}}";

        Type webresponseType = Types.newParameterizedType(WebResponse.class, User.class);
        JsonAdapter<WebResponse<User>> jsonAdapter = moshi.adapter(webresponseType);
        WebResponse<User> response = jsonAdapter.fromJson(json);

        User expected = User.with("Ryan", "Harter");
        assertEquals(expected, response.data());
        assertEquals(expected, response.dataList().get(0));
        assertEquals(expected, response.dataMap().get("key").get(0));
    }

}
