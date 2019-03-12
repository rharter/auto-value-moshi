package com.ryanharter.auto.value.moshi.test;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.List;
import java.util.Map;

/** Showcases inclusion of all standard types. */
@AutoValue public abstract class StandardObject {
  public static JsonAdapter<StandardObject> jsonAdapter(Moshi moshi) {
    return new AutoValue_StandardObject.MoshiJsonAdapter(moshi);
  }

  public abstract boolean aBoolean();

  public abstract byte aByte();

  public abstract char aChar();

  public abstract double aDouble();

  public abstract float aFloat();

  public abstract int aInt();

  public abstract long aLong();

  public abstract short aShort();

  public abstract Boolean aBooleanObj();

  public abstract Byte aByteObj();

  public abstract Character aCharacterObj();

  public abstract Double aDoubleObj();

  public abstract Float aFloatObj();

  public abstract Integer aIntegerObj();

  public abstract Long aLongObj();

  public abstract Short aShortObj();

  public abstract String aString();

  public abstract List<String> aList();

  public abstract Map<String, Integer> aMap();

  public abstract AnEnum anEnum();

  public abstract int[] anArray();

  enum AnEnum {
    VALUE_1
  }
}
