package com.ryanharter.auto.value.moshi.example.hexcolor;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

/** Converts strings like #ff0000 to the corresponding color ints. */
class ColorAdapter {
  @ToJson String toJson(@HexColor int rgb) {
    return String.format("#%06x", rgb);
  }

  @FromJson @HexColor int fromJson(String rgb) {
    return Integer.parseInt(rgb.substring(1), 16);
  }
}