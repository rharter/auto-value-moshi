# AutoValue: Moshi Extension

[![Build Status](https://travis-ci.org/rharter/auto-value-moshi.svg?branch=master)](https://travis-ci.org/rharter/auto-value-moshi)

An extension for Google's [AutoValue](https://github.com/google/auto) that creates a simple [Moshi](https://github.com/square/moshi) JsonAdapterFactory for each AutoValue annotated object.

## Usage

Simply include auto-value-moshi in your project and annotate your target autovalue class with Moshi's
`@JsonClass` annotation. `generateAdpater` must be true, and the `generator` property value should
be `"avm"`.

```java
@JsonClass(generateAdapter = true, generator = "avm")
@AutoValue
public abstract class Foo {
  abstract String bar();
  @Json(name="Baz") abstract String baz();

  public static JsonAdapter<Foo> jsonAdapter(Moshi moshi) {
    return new AutoValue_Foo.MoshiJsonAdapter(moshi);
  }
}
```

Using `@JsonClass`, no further configuration is necessary. Moshi 1.9+ will automatically pick these
types up at runtime.

### _Legacy alternative_

Add a public static method with the following signature to classes you want to get Moshi 
`JsonAdapter`s. You can also annotate your properties using `@Json` to define an alternate name 
for de/serialization.

```java
@AutoValue public abstract class Foo {
  abstract String bar();
  @Json(name="Baz") abstract String baz();

  public static JsonAdapter<Foo> jsonAdapter(Moshi moshi) {
    return new AutoValue_Foo.MoshiJsonAdapter(moshi);
  }
}
```

Now build your project and de/serialize your Foo.

## Generics support

_note: this section only applies if using the legacy opt-in via static method. If using `@JsonClass`, Moshi will handle this automatically_.

If the annotated class uses generics, the static method needs a little modification. Simply add a `Type[]` parameter and pass it to the generated `MoshiJsonAdapter` class.

```java
@AutoValue public abstract class Foo<T> {
    abstract T data();
    
    public static <T> JsonAdapter<Foo<T>> jsonAdapter(Moshi moshi, Type[] types) {
        return new AutoValue_Foo.MoshiJsonAdapter(moshi, types);
    }
}
```

## Builder Support
If your `@AutoValue` class has a builder, auto-value-moshi will use the builder to 
instantiate the class. If the `@AutoValue` class has a static no-argument factory method for its builder, it will be used. If there are multiple factory methods, the one annotated `@AutoValueMoshiBuilder` will be used. This can be 
useful for setting default values.

```java
@JsonClass(generateAdapter = true, generator = "avm")
@AutoValue
public abstract class Foo {
  abstract int bar();
  abstract String quux();

  public static Builder builder() {
    return new AutoValue_Foo.Builder();
  }

  @AutoValueMoshiBuilder
  public static Builder builderWithDefaults() {
    return new builder().quux("QUUX");
  }
}
```

## Factory

_note: this section only applies if using the legacy opt-in via static method. If using `@JsonClass`, Moshi will handle this automatically_.

Optionally, auto-value-moshi can create a single [JsonAdapter.Factory](http://square.github.io/moshi/1.x/moshi/com/squareup/moshi/JsonAdapter.Factory.html) so
that you don't have to add each generated JsonAdapter to your Moshi instance manually.

To generate a `JsonAdapter.Factory` for all of your auto-value-moshi classes, simply create
an abstract class that implements `JsonAdapter.Factory` and annotate it with `@MoshiAdapterFactory`,
and auto-value-moshi will create an implementation for you.  You simply need to provide a static
factory method, just like your AutoValue classes, and you can use the generated `JsonAdapter.Factory`
to help Moshi de/serialize your types.

```java
@MoshiAdapterFactory
public abstract class MyAdapterFactory implements JsonAdapter.Factory {

  // Static factory method to access the package
  // private generated implementation
  public static JsonAdapter.Factory create() {
    return new AutoValueMoshi_MyAdapterFactory();
  }
  
}
```

Then you simply need to register the Factory with Moshi.

```java
Moshi moshi = new Moshi.Builder()
    .add(MyAdapterFactory.create())
    .build();
```

## Transient types

To ignore certain properties from serialization, you can use the `@AutoTransient` annotation. This comes from a 
shared transience annotations library and is an `api` dependency of the runtime artifact. You can annotate
a property and it will be treated as `transient` for both serialization and deserialization. Note that
this should only be applied to nullable properties.

## Download

Add a Gradle dependency:

```kotlin
annotationProcessor("com.ryanharter.auto.value:auto-value-moshi:0.4.6")
implementation("com.ryanharter.auto.value:auto-value-moshi-annotations:0.4.6")
```

## License

```
Copyright 2015 Ryan Harter.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
