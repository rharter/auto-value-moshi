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

final Moshi moshi = new Moshi.Builder()
    .add(new AutoValueMoshiAdapterFactory())
    .build();
```

Now build your project and de/serialize your Foo.

In addition to generating implementations of your `@AutoValue` annotated classes, auto-value-moshi also generates an `AutoValueMoshiAdapterFactory` class which you can register with Moshi to automatically add all of your generated JsonAdapters.

## Download

Add a Gradle dependency:

```groovy
apt 'com.ryanharter.auto.value:auto-value-moshi:0.3.3-rc1'
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
