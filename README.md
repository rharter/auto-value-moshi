# AutoValue: Moshi Extension

[![Build Status](https://travis-ci.org/rharter/auto-value-moshi.svg?branch=master)](https://travis-ci.org/rharter/auto-value-moshi)

An extension for Google's [AutoValue](https://github.com/google/auto) that creates a simple [Moshi](https://github.com/square/moshi) JsonAdapterFactory for each AutoValue annotated object.

## Usage

Simply include auto-value-moshi in your project and add a public static method with the following
signature to classes you want to get Moshi `JsonAdapter`s. You can also annotate your properties
using `@Json` to define an alternate name for de/serialization.

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

## Factory

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

## Download

Add a Gradle dependency:

```groovy
apt 'com.ryanharter.auto.value:auto-value-moshi:0.4.3'

// if you use the @MoshiAdapterFactory annotation, you'll need to add the 'annotations' artifact 
// as a provided dependency:
provided 'com.ryanharter.auto.value:auto-value-moshi-annotations:0.4.3'
```

(Using the [android-apt](https://bitbucket.org/hvisser/android-apt) plugin)

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
