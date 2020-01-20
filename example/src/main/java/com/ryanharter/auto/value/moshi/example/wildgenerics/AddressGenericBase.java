package com.ryanharter.auto.value.moshi.example.wildgenerics;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.moshi.example.Address;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "avm")
@AutoValue
public abstract class AddressGenericBase
    extends GenericBase<Address> {

  public abstract String topLevelNonGeneric();

  public static Builder builder() {
    return new AutoValue_AddressGenericBase.Builder();
  }

  @AutoValue.Builder public abstract static class Builder
      implements GenericBase.Builder<Address, Builder> {
    public abstract Builder topLevelNonGeneric(String topLevel);
    public abstract AddressGenericBase build();
  }
}
