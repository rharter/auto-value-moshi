# Change Log

## Version 0.4.4 (2017-10-03)

* Use getMethod() instead of getDeclaredMethod(). (#94)
* Add support for generic types with JsonQualifiers (#92)
* use annotationProcessor instead of apt for gradle (#91)
* Only match on actual return type, not any parameterized type (#85)
* Bump dependencies and include `idea` plugin (#84)
* Call value method only once if property is nullable. (#81)
* Set auto-service to compileOnly (#80)

## Version 0.4.3 (2017-02-13)

* Doesn't generate redundant static JsonAdapter method. (#72)
* Leverage Moshi 1.4's `select()` API for better performance. (#76)
* Make generated factory request null save adapters. (#71)
* Handle wildcard types in generics. (#78)

## Version 0.4.2 (2016-11-03)

#### Supports: AutoValue 1.3

* Generated factory ignores annotated types.
* Reduces the number of JsonAdapter ClassName lookups.
* Moves `@MoshiAdapterFactory` into a separate artifact.
* Adds support for generics.

## Version 0.4.1 (2016-10-17)

#### Supports: AutoValue 1.3

* Fixes generated adapter method for annotated methods. (#57)

## Version 0.4.0 (2016-09-18)

#### Supports: AutoValue 1.3

* Updates extension to support AutoValue 1.3 (#50)
* Properly handle uppercase package names. (#52)

## Version 0.4.0-rc2 (2016-07-20)

#### Supports: AutoValue 1.3-rc2

* Updates extension to support AutoValue 1.3-rc2
* Adds `@MoshiAdapterFactory` annotation to create a `JsonAdapter.Factory` for all auto-value-moshi types. (Thanks @hzsweers)

## Version 0.3.3-rc1 (2016-06-13)

#### Supports: AutoValue 1.3-rc1

* Updates extension to support AutoValue 1.3-rc1
* Fixes issue when using `JsonAdapters` with the same name. (fixes #36)

## Version 0.3.2 (2016-05-18)

#### Supports: AutoValue 1.2

* Reverted 0.3.1 with breaking change.

## Version 0.3.1 (2016-05-18)

#### Supports: AutoValue 1.2

* Fixes issue causing type comparison to fail in type adapter. 

## Version 0.3.0 (2016-05-06)

#### Supports: AutoValue 1.2

* Adds a generator to create a JsonAdapter.Factory which includes all applicable `@AutoValue` annotated class JsonAdapters. (#30)
* Cleans up JsonAdapter support and adds exclusion support. (#23)
* Adds support for primitive defaults in the adapters. (#27)

## Version 0.2.2 (2016-04-05)

#### Supports: AutoValue 1.2-rc1

* Adds support for custom type serializiation with [@JsonQualifier](https://github.com/square/moshi#alternate-type-adapters-with-jsonqualifier)
* Fixes issue causing method prefixes (`get`, `is`) to be ignored.

## Version 0.2.1 (2016-03-22)

Fixes snapshot issues with 0.2.0 release. Only guaranteed to support AutoValue 1.2-rc1

## Version 0.2.0 (2016-03-21)

Initial release. Only guaranteed to support AutoValue 1.2-rc1
