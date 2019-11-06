package com.ryanharter.auto.value.moshi.example;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BirthDateAdapter {
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @FromJson
    public Date fromJson(String dateString) {
        try {
            return df.parse(dateString);
        } catch (ParseException e) {
            return new Date();
        }
    }

    @ToJson
    public String toJson(Date date) {
        return df.format(date);
    }
}
