# AutoValue: Moshi Extension

[![Build Status](https://travis-ci.org/rharter/auto-value-moshi.svg?branch=master)](https://travis-ci.org/rharter/auto-value-moshi)

An extension for Google's [AutoValue](https://github.com/google/auto) that creates a simple [Moshi](https://github.com/square/moshi) JsonAdapterFactory for each AutoValue annotated object.

## Usage

Simply include auto-value-moshi in your project and add the generated JsonAdapterFactory to your Moshi instance.  You can also annotate your properties using `@Json` to define an alternate name for de/serialization.

```java
@AutoValue public abstract class Foo {
  abstract String bar();
  @Json(name="Baz") abstract String baz();

  public static JsonAdapter.Factory typeAdapterFactory() {
    return AutoValue_Foo.typeAdapterFactory();
  }
}

final Moshi moshi = new Moshi.Builder()
    .add(Foo.typeAdapterFactory())
    .build();
```

Now build your project and de/serialize your Foo.

## TODO

This wouldn't be quite complete without some added features.

* Automatic registration
* Default value support

## Download

Add a Gradle dependency:

```groovy
apt 'com.ryanharter.auto.value:auto-value-moshi:0.2.0'
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
